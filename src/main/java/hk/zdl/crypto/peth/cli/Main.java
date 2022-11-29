package hk.zdl.crypto.peth.cli;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.TransactionBroadcast;
import signumj.entity.response.http.BRSError;
import signumj.service.NodeService;
import signumj.util.SignumUtils;

public class Main {

	static {
		SignumUtils.addAddressPrefix("TS");
	}

	public static final int peth_decimals = 4;
	private static final String default_url = "http://mainnet.peth.world:6876";

	public static void main(String[] args) throws Throwable {
		var jobj_arg = new JSONObject(new JSONTokener(System.in));
		var url = jobj_arg.optString("url", default_url);
		var action = jobj_arg.optString("action", "balance");
		if (action.equals("balance")) {
			var address = jobj_arg.getString("address");
			NodeService ns = NodeService.getInstance(url);
			try {
				BigInteger qnt = ns.getAccount(SignumAddress.fromEither(address)).toFuture().get().getBalance().toNQT();
				System.out.println(new BigDecimal(qnt, peth_decimals));
				System.exit(0);
			} catch (InterruptedException | ExecutionException e) {
				if (e.getCause().getClass().equals(signumj.entity.response.http.BRSError.class)) {
					if (((BRSError) e.getCause()).getCode() == 5) {
						System.out.println(0);
						System.exit(0);
					}
				}
			}
		} else if (action.equals("send")) {
			var recipient = jobj_arg.getString("recipient");
			var amount = jobj_arg.getBigDecimal("amount");
			var fee = jobj_arg.getBigDecimal("fee");
			var msg = jobj_arg.optString("msg");

			var private_key = new byte[0];
			if (jobj_arg.has("memo")) {
				private_key = SignumCrypto.getInstance().getPrivateKey(jobj_arg.getString("memo"));
			} else if (jobj_arg.has("private_key_base64")) {
				private_key = Base64.decode(jobj_arg.getString("private_key_base64"));
			} else if (jobj_arg.has("private_key_hex")) {
				private_key = Hex.decode(jobj_arg.getString("private_key_hex"));
			}
			var public_key = SignumCrypto.getInstance().getPublicKey(private_key);

			NodeService ns = NodeService.getInstance(url);
			byte[] unsignedTransaction;
			if (msg.isBlank()) {
				unsignedTransaction = ns.generateTransaction(SignumAddress.fromEither(recipient), public_key, toSignumValue(amount), toSignumValue(fee), 1440, null).blockingGet();
			} else {
				unsignedTransaction = ns.generateTransactionWithMessage(SignumAddress.fromEither(recipient), public_key, toSignumValue(amount), toSignumValue(fee), 1440, msg, null).blockingGet();
			}
			byte[] signedTransactionBytes = SignumCrypto.getInstance().signTransaction(private_key, unsignedTransaction);
			TransactionBroadcast x = ns.broadcastTransaction(signedTransactionBytes).blockingGet();
			System.out.println("txid:" + x.getTransactionId().getID());

		} else if (action.equals("get_account")) {
			var account = SignumAddress.fromEither(jobj_arg.getString("address"));
			System.out.println("T" + account.getFullAddress());
			System.out.println(account.getID());
		} else if (action.equals("get_address")) {
			var memo = jobj_arg.optString("memo", null);
			var private_key_str = jobj_arg.optString("private_key", null);
			var public_key_str = jobj_arg.optString("public_key", null);

			var private_key = new byte[] {};
			var public_key = new byte[] {};

			if (memo != null) {
				private_key = SignumCrypto.getInstance().getPrivateKey(memo);
			} else if (private_key_str != null) {
				private_key = Hex.decode(private_key_str);
			}
			if (public_key_str != null) {
				public_key = Hex.decode(public_key_str);
			} else {
				public_key = SignumCrypto.getInstance().getPublicKey(private_key);
			}
			if (jobj_arg.optBoolean("show_id")) {
				var id = SignumCrypto.getInstance().getAddressFromPublic(public_key).getID();
				System.out.println(id);
			} else {
				var address = SignumCrypto.getInstance().getAddressFromPublic(public_key).getFullAddress();
				System.out.println("T" + address);
			}
		} else if (action.equals("get_tx")) {
			var account = SignumAddress.fromEither(jobj_arg.getString("address")).getID();
			var confs = jobj_arg.optInt("confs", 0);
			var msg = jobj_arg.optString("msg", null);

			var get = new HttpGet(url + "/burst?requestType=getAccountTransactions&type=0&subtype=0&account=" + account + "&numberOfConfirmations=" + confs);
			var response = HttpClientBuilder.create().build().execute(get);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			var content = EntityUtils.toString(response.getEntity());
			if (statusCode != 200) {
				System.exit(-1);
			} else {
				var jobj = new JSONObject(new JSONTokener(content));
				if (jobj.optInt("errorCode", 0) != 0) {
					System.out.println(content);
					System.exit(-1);
				} else {
					var txs = jobj.getJSONArray("transactions");
					var jarr = new JSONArray();
					for (int i = 0; i < txs.length(); i++) {
						jobj = txs.getJSONObject(i);
						if (msg == null) {
							jarr.put(jobj);
						} else if (jobj.has("attachment")) {
							var o = jobj.getJSONObject("attachment");
							if (o.getBoolean("messageIsText") && o.optString("message", "").equals(msg)) {
								jarr.put(jobj);
							}
						}
					}
					System.out.println(jarr);
				}
			}
		}
	}

	private static final SignumValue toSignumValue(BigDecimal amount) {
		return SignumValue.fromNQT(amount.multiply(BigDecimal.TEN.pow(peth_decimals)).toBigInteger());
	}

}
