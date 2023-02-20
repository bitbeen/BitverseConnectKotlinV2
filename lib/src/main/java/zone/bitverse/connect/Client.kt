package zone.bitverse.connect

val LOGO_URI: String = "https://www.gitbook.com/cdn-cgi/image/width=24,height=24,fit=contain,dpr=2,format=auto/https%3A%2F%2F3322270121-files.gitbook.io%2F~%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252FDanFaxtDexYmvoozseSd%252Ficon%252F1qCpdw3NV9ENaEBf2lrj%252FGroup%252048095651.png%3Falt%3Dmedia%26token%3D2faba148-1893-479e-b2c6-4dd1b162d89a"

interface Client {
    data class Config(
        val bridge: String,
        var topic: String?,
        var key: String?,
        var peerId: String?,
    )

    fun ethSign(message: String, account: String,callback: (Session.MethodCall.Response) -> Unit)
    fun personalSign(message: String, account: String,callback: (Session.MethodCall.Response) -> Unit)
    fun ethSignTypedData(message: String, account: String,callback: (Session.MethodCall.Response) -> Unit)
    fun ethSendTransaction(transaction: Transaction,callback: (Session.MethodCall.Response) -> Unit)
    fun serialize(): Config?
    fun transport(): Session.Transport?

    fun connect(
        config: Config,
        callback: (Session.MethodCall.Response) -> Unit
    ): String

    fun performMethodCall(
        call: Session.MethodCall,
        callback: ((Session.MethodCall.Response) -> Unit)?
    )

    fun disconnect()
    fun reconnectIfNeeded()
}
