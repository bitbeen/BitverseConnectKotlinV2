# bitverseConnectKotlin

[![](https://jitpack.io/v/bitverseWallet/bitverseConnectKotlin.svg)](https://jitpack.io/#bitbeen/BitverseConnectKotlin)

library to use bitverseConnect with Kotlin or Java

## 添加SDK依赖
将@jitpack添加到gradle文件中

```gradle
repositories {
 ...
 maven { url 'https://jitpack.io' }
}
```
添加sdk依赖

从<https://jitpack.io/#bitbeen/BitverseConnectKotlin>查找最新版本号

```gradle
dependencies {
 implementation 'com.github.bitbeen:BitverseConnectKotlin:1.1.0'
}
```

## 使用方式

### 初始化bitverseConnectApi
```kotlin
val connection = BitverseConnectApi(object :bitverseConnectDelegate{
        override fun didConnect(chainId: Int?, accounts: String?) {
            TODO("连接成功，返回chain id 和钱包地址")
        }

        override fun didDisconnect() {
            TODO("连接断开")
        }

        override fun failedToConnect() {
            TODO("连接失败")
        }

    })
```

### 连接钱包
需要配置deeplink，用于Bitverse App处理完事务后，返回当前App
```xml
<intent-filter >
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <!-- Accepts URIs that begin with "example://gizmos” -->
    <data android:scheme="bitversedapp"
        android:host="wc" />
</intent-filter>
```
调用连接钱包API
```kotlin
connection.connect(
        context = requireContext(),
        dappName = "Example App",
        dappDescription = "bitverseconnect_android_example",
        dappUrl = "https://example.com",
        icons = listOf("用于bitverse App展示dapp的图片"),
        callbackUrl = "bitversedapp://wallet", // 设置返回当前App的deeplink
    )
```

### 签名
SDK 支持 eth sign 、personal sign 和 signTypedData
> eth_sign 是危险操作，会导致资金丢失，bitverse Wallet 已经把 eth_sign 封禁

```kotlin
connection.personalSign(
            requireContext(),
            message = "0xff",
            account = "钱包地址"
        ) {resp ->
            // 回调方式位于子线程，务必切换到UI线程进行UI展示
            uiScope.launch {    
                binding.txRespData.text = it.result.toString()
            }
        }
```

### 交易
```kotlin
connection.ethSendTransaction(
        requireContext(),
        Transaction(
            from = binding.txAddress.text.toString(),
            to = binding.txAddress.text.toString(),
            nonce = null,
            gasPrice = null,
            gasLimit = null,
            value = "0xff",
            data = "0x",
        )
    ) {
        uiScope.launch {
            binding.txRespData.text = it.result.toString()
        }
    }
```
## typed sign签名
```kotlin
connection.ethSignTypedData(
    requireContext(),
    message = "{\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallet\",\"type\":\"address\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person\"},{\"name\":\"contents\",\"type\":\"string\"}]},\"primaryType\":\"Mail\",\"domain\":{\"name\":\"Ether Mail\",\"version\":\"1\",\"chainId\":1,\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"},\"message\":{\"from\":{\"name\":\"Cow\",\"wallet\":\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"},\"to\":{\"name\":\"Bob\",\"wallet\":\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"},\"contents\":\"Hello, Bob!\"}}",
    account = it
){
        uiScope.launch {
            binding.txRespData.text = it.result.toString()
        }
    }


```