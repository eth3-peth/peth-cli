# peth-cli

命令行的PETH操作工具，目前支持的操作：查看给定地址的余额、转账、从公钥/私钥/助记词生成地址。

操作方式：命令行：java -jar peth-cli.jar
参数：JSON格式，作为管道输入给到程序

## 示例：

### 生成地址：

`{"action":"get_address","memo":"some words here","public_key":"FFFFFFFF","private_key":"FFFFFFFF"}`

返回值：钱包地址，格式：TS-Z9JQ-9B3P-T6CU-9LFA4

* 其中，助记词、公钥、私钥提供其中之一即可，若提供多个参数，只选其一，优先级：公钥>私钥>助记词；助记词大小写敏感，公钥、私钥为HEX格式，大小写不敏感。

* 其中，加上  show_id:true  参数，则返回数字格式的钱包ID，而非字母格式的钱包地址。

### 获取钱包ID/地址

`{"action":"get_account","address":"TS-8AJJ-9TMN-M7J5-AJ8S6"}`
返回值：钱包地址、数字ID，各一行

* 其中，address可以为TS-开头的钱包地址或数字ID，均可。

### 查看余额：

`{"action":"balance","address":"TS-Z9JQ-9B3P-T6CU-9LFA4"}`

返回值：当前钱包余额，带四位小数

### 转账：
`{"action":"send","recipient":"TS-Z9JQ-9B3P-T6CU-9LFA4","amount":"1.0","fee":"1.0","memo":"some words here"}`

返回值：交易ID，格式：txid:1234567890

* 其中，私钥的提供方式可以为：助记词（参数名：memo）、Base64（参数名：private_key_base64）、HEX（参数名：private_key_hex）

转账时可以加上msg参数发送带有明文文本消息的交易

### 查看已到账的转账记录
`{"url":"http://testnet.peth.world:6876/","action":"get_tx","address":"TS-DQP5-DVXJ-AJSX-EMV7Q","msg":"54321",confs:0}`

* 其中，url为节点服务器地址（可选项），address为所查看的钱包地址或数字ID；若msg非空，则仅紧查看留言消息为msg的转账记录；若confs参数非空且大于零，则仅查看确认数目大于confs的交易记录。