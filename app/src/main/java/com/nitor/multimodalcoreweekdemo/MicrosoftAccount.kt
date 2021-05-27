package com.nitor.multimodalcoreweekdemo

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.microsoft.graph.concurrency.ICallback
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.models.extensions.DriveItem
import com.microsoft.graph.models.extensions.IGraphServiceClient
import com.microsoft.graph.models.extensions.WorkbookNamedItem
import com.microsoft.graph.models.extensions.WorkbookRange
import com.microsoft.graph.requests.extensions.GraphServiceClient
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.URLEncoder
import java.time.Instant

private const val TAG:String = "MicrosoftAccount"
private val SCOPES = arrayOf("User.Read", "User.ReadBasic.All", "Files.Read", "Files.Read.All");
private val IRON_BANK_DRIVE_ID = "b!vjRX6AeDXEOxr_RA9ux14unqSpbFThZDmJ5KJUnvma3fPFPEUAQuRLdMnTydRvNk"

class MicrosoftAccount(applicationContext: Context, private val accountCallback: MicrosoftAccountCallback) {

    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var mActiveAccount: IAccount? = null
    private var mAuthenticationResult: IAuthenticationResult? = null

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(applicationContext,
            R.raw.auth_config_single_account, object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    loadAccountAsync()
                }

                override fun onError(ex: MsalException) {
                    Log.e(TAG, "Create single account public client application failed", ex)
                }
        })
    }

    private fun loadAccountAsync() {
        if (mSingleAccountApp == null) {
            return
        }

        mSingleAccountApp!!.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                mActiveAccount = activeAccount
                mAuthenticationResult = null
                accountCallback.onAccountLoaded(mActiveAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                mActiveAccount = currentAccount
                mAuthenticationResult = null
                accountCallback.onAccountLoaded(currentAccount)
            }

            override fun onError(exception: MsalException) {

            }
        })
    }

    fun getAccessToken(): String? {
        if (mSingleAccountApp == null || mActiveAccount == null) {
            return null
        }

        val now = Instant.now().minusMillis(60000)
        if (mAuthenticationResult == null || mAuthenticationResult!!.expiresOn.toInstant().isBefore(now)) {
            mAuthenticationResult = mSingleAccountApp!!.acquireTokenSilent(SCOPES, mActiveAccount!!.authority)
        }
        return mAuthenticationResult?.accessToken
    }

    fun getFullName(): String? {
        if (mSingleAccountApp == null || mActiveAccount == null) {
            return null
        }

        return mActiveAccount?.claims?.get("name").toString()
    }

    fun getGraphServiceClient(): IGraphServiceClient? {
        val accessToken: String? = getAccessToken()
        if (accessToken != null) {
            return GraphServiceClient
                .builder()
                .authenticationProvider { request ->
                    request.addHeader("Authorization", "Bearer $accessToken")
                }
                .buildClient()
        }
        return null
    }

    fun loadProfilePicture(callback: ICallback<InputStream>) {
        if (mActiveAccount == null) {
            return
        }

        GlobalScope.launch(Dispatchers.Default) {
            val client = getGraphServiceClient()
            if (client != null) {
                client
                    .me()
                    .photo()
                    .content()
                    .buildRequest()[callback]
            }
        }
    }

    fun queryIronBankBalance(callback: ICallback<String>) {
        if (mActiveAccount == null) {
            return
        }

        GlobalScope.launch(Dispatchers.Default) {
            val client = getGraphServiceClient()!!
            val ironBankFile = Uri.encode(mActiveAccount?.claims?.get("name")
                .toString()
                .split(" ")
                .reversed()
                .joinToString(" ", postfix = ".xlsx"), "UTF-8")

            client
                .me()
                .drives(IRON_BANK_DRIVE_ID)
                .root()
                .itemWithPath(ironBankFile)
                .buildRequest()[object: ICallback<DriveItem> {
                override fun success(result: DriveItem?) {
                    client
                        .me()
                        .drives(IRON_BANK_DRIVE_ID)
                        .items(result?.id)
                        .workbook()
                        .names("balance")
                        .range()
                        .buildRequest()[object: ICallback<WorkbookRange> {
                        override fun success(result: WorkbookRange?) {
                            if (result?.rowCount == 1 && result?.columnCount == 1) {
                                callback.success(result.values.asJsonArray[0].asJsonArray[0].asString)
                            }
                        }

                        override fun failure(ex: ClientException?) {
                            Log.e(TAG, "Load iron bank balance failed")
                            callback.failure(ex)
                        }
                        }]
                }

                override fun failure(ex: ClientException?) {
                    Log.e(TAG, "Load iron bank file metadata failed")
                }
                }]
        }
    }

    fun signIn(activity: Activity) {
        if (mSingleAccountApp != null) {
            mSingleAccountApp!!.signIn(activity,
                null,
                SCOPES,
                Prompt.CONSENT,
                object: AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        /* Successfully got a token, use it to call a protected resource - MSGraph */
                        Log.d(TAG, "Successfully authenticated")
                        mActiveAccount = authenticationResult.account
                        mAuthenticationResult = authenticationResult
                        accountCallback.onAccountLoaded(mActiveAccount)
                    }

                    override fun onError(ex: MsalException) {
                        /* Failed to acquireToken */
                        Log.e(TAG, "Authentication failed")
                    }

                    override fun onCancel() {
                        /* User canceled the authentication */
                        Log.i(TAG, "User cancelled login.")
                    }
                })
        }
    }

    fun signOut() {
        if (mSingleAccountApp != null) {
            mSingleAccountApp!!.signOut(object :
                ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    mActiveAccount = null
                    mAuthenticationResult = null
                    accountCallback.onAccountLoaded(null)
                }

                override fun onError(ex: MsalException) {
                    Log.e(TAG, "Sign out failed")
                }
            })
        }
    }

    interface MicrosoftAccountCallback {

        fun onAccountLoaded(account: IAccount?)

    }
}