package zone.bitverse.connect

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.squareup.moshi.Moshi
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import okhttp3.OkHttpClient
import zone.bitverse.connect.impls.BCClient
import zone.bitverse.connect.impls.MoshiPayloadAdapter
import zone.bitverse.connect.impls.OkHttpTransport
import java.net.URLEncoder

object BitverseConnectApi {
    private var client: Client? = null
    private var url: String? = null
    private var appContext : Application? = null
    private var delegate: BitverseConnectDelegate? = null
    const val TAG = "BitverseConnectApi"

  fun initilize(application: Application,
                dappName: String,
                dappDescription: String,
                dappUrl: String,
                icons: List<String>,
                callbackUrl: String,
                delegate: BitverseConnectDelegate?) {
    appContext = application
    this.delegate = delegate;
    val serverUri = "wss://relay.walletconnect.com?projectId=1c8433f67cf7bfd3e7f9e169118802ce"
    CoreClient.initialize(
      relayServerUrl = serverUri, connectionType = ConnectionType.AUTOMATIC, application = application, metaData = Core.Model.AppMetaData(
        name = dappName,
        description = dappDescription,
        url = dappUrl,
        icons = icons,
        redirect = callbackUrl
      )
    ) {}

    val init = Sign.Params.Init(core = CoreClient)

    SignClient.initialize(init) { error ->
      // Error will be thrown if there's an issue during initialization
    }

    val dappDelegate = object : SignClient.DappDelegate {
      override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) {
        // Triggered when Dapp receives the session approval from wallet

        val componets = approvedSession.accounts.first().split(':')
        val size = componets.count()
        if (size > 1) {
          delegate?.didConnect(componets[size - 2].toInt(), listOf(componets[size - 1]))
        } else {
          delegate?.failedToConnect()
        }
      }

      override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) {
        // Triggered when Dapp receives the session rejection from wallet
        delegate?.didDisconnect()
      }

      override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) {
        // Triggered when Dapp receives the session update from wallet
      }

      override fun onSessionExtend(session: Sign.Model.Session) {
        // Triggered when Dapp receives the session extend from wallet
      }

      override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) {
        // Triggered when the peer emits events that match the list of events agreed upon session settlement
      }

      override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        // Triggered when Dapp receives the session delete from wallet
      }

      override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) {
        // Triggered when Dapp receives the session request response from wallet
      }

      override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        //Triggered whenever the connection state is changed
        when(state.isAvailable) {
          false -> delegate?.didDisconnect()
          else -> print("connected")
        }

      }

      override fun onError(error: Sign.Model.Error) {
        // Triggered whenever there is an issue inside the SDK
      }
    }

    SignClient.setDappDelegate(dappDelegate)
  }

    /**
     * dappName：dapp的名称
     * dappDescription：dapp的描述
     * dappUrl：dapp对应的链接
     * bitverseDapp：当前app的deeplink或者Universal Links，以便Bitverse产生相关结果后可以回调回到当前app
     */
    fun connect(
        context: Context,
        dappName: String,
        dappDescription: String,
        dappUrl: String,
        icons: List<String>,
        callbackUrl: String
    ) {
      val pairing: Core.Model.Pairing =
        CoreClient.Pairing.create() { error ->
           delegate?.didDisconnect()
        }!!
       val namespace: String = "eip155"
       val chains: List<String> = listOf("eip155:1")
       val methods: List<String> = listOf("eth_sendTransaction", "personal_sign", "eth_sign", "eth_signTypedData")
       val events: List<String> = listOf("chainChanged", "accountsChanged")
       val requiredNamespaces: Map<String, Sign.Model.Namespace.Proposal> = mapOf(namespace to Sign.Model.Namespace.Proposal(chains, methods, events)) /*Required namespaces to setup a session*/
       //val pairing: Core.Model.Pairing = /*Either an active or inactive pairing*/
      val connectParams = Sign.Params.Connect(requiredNamespaces, null, null, pairing)

      SignClient.connect(connectParams,
        onSuccess = {
          // Get a handler that can be used to post to the main thread
          val mainHandler = Handler(context.mainLooper)
          val myRunnable = object: Runnable {
            override fun run() {
              val wsURL = pairing.uri
              Log.e(TAG, "wsURL:" + wsURL)
              var url = "https://bitverseapp.page.link/?apn=com.bitverse.app&afl=https://bitverse.zone/download?deeplink%3Dbitverseapp://open/wallet&isi=1645515614&ibi=com.bitverse.app&link=https://bitverse.zone/download?deeplink%3Dbitverseapp://open/wallet?uri="+URLEncoder.encode(URLEncoder.encode(wsURL));
              url += URLEncoder.encode(URLEncoder.encode("&callbackUrl=${URLEncoder.encode(callbackUrl)}"));
              val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
              }
              context.startActivity(intent)
            } // This is your code
          };
          mainHandler.post(myRunnable);
        },
        onError = {
          delegate?.failedToConnect()
        }
      )
    }

    fun ethSign(
        context: Context,
        message: String,
        account: String,
        callback: (Session.MethodCall.Response) -> Unit
    ) {
        client?.ethSign(message, account, callback)
        startBitverseApp(context)
    }

    fun personalSign(
        context: Context,
        message: String,
        account: String,
        callback: (Session.MethodCall.Response) -> Unit
    ) {
        client?.personalSign(message, account, callback)
        startBitverseApp(context)
    }

    fun ethSignTypedData(
        context: Context,
        message: String,
        account: String,
        callback: (Session.MethodCall.Response) -> Unit
    ) {
        client?.ethSignTypedData(message, account, callback)
        startBitverseApp(context)
    }

    fun ethSendTransaction(
        context: Context,
        transaction: Transaction,
        callback: (Session.MethodCall.Response) -> Unit
    ) {
        client?.ethSendTransaction(transaction, callback)
        startBitverseApp(context)
    }

    fun disconnect() {
        client?.disconnect()
    }

    fun reconnectIfNeeded() {
        client?.reconnectIfNeeded()
    }

    private fun startBitverseApp(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("bitverseapp://open/wallet")
        }
        context.startActivity(intent)
    }

}
