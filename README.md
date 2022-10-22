# peth-cli

命令行的PETH操作工具，目前支持的操作：查看给定地址的余额、转账、从公钥/私钥/助记词生成地址。

操作方式：命令行：java -jar peth-cli.jar
参数：JSON格式，作为管道输入给到程序

示例：

查看余额：
{"action":"balance","address":"TS-Z9JQ-9B3P-T6CU-9LFA4"}
返回值：当前钱包余额，带四位小数

转账：
{"action":"send","recipient":"TS-Z9JQ-9B3P-T6CU-9LFA4","amount":"1.0","fee":"1.0","memo":"some words here"}
返回值：交易ID，格式：txid:1234567890

其中，私钥的提供方式可以为：助记词（参数名：memo）、Base64（参数名：private_ket_base64）、HEX（参数名：private_ket_hex）

生成地址：
{"action":"get_address","memo":"some words here","public_key":"FFFFFFFF","private_key":"FFFFFFFF"}
返回值：钱包地址，格式：TS-Z9JQ-9B3P-T6CU-9LFA4

其中，助记词、公钥、私钥提供其中之一即可，若提供多个参数，只选其一，优先级：公钥>私钥>助记词；助记词大小写敏感，公钥、私钥为HEX格式，大小写不敏感。