package hk.zdl.crypto.peth.cli;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
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
	private static final String default_url = "http://peth.world:6876";

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

			var private_key = new byte[0];
			if (jobj_arg.has("memo")) {
				private_key = SignumCrypto.getInstance().getPrivateKey(jobj_arg.getString("memo"));
			} else if (jobj_arg.has("private_ket_base64")) {
				private_key = Base64.decode(jobj_arg.getString("private_ket_base64"));
			} else if (jobj_arg.has("private_ket_hex")) {
				private_key = Hex.decode(jobj_arg.getString("private_ket_hex"));
			}
			var public_key = SignumCrypto.getInstance().getPublicKey(private_key);

			NodeService ns = NodeService.getInstance(url);
			byte[] unsignedTransaction = ns.generateTransaction(SignumAddress.fromRs(recipient), public_key, toSignumValue(amount), toSignumValue(fee), 1440, null).blockingGet();
			byte[] signedTransactionBytes = SignumCrypto.getInstance().signTransaction(private_key, unsignedTransaction);
			TransactionBroadcast x = ns.broadcastTransaction(signedTransactionBytes).blockingGet();
			System.out.println("txid:" + x.getTransactionId().getID());

		} else if (action.equals("get_address")) {
			var memo = jobj_arg.optString("memo", null);
			var private_key_str = jobj_arg.optString("private_key", null);
			var public_key_str = jobj_arg.optString("public_key", null);

			var private_key = new byte[] {};
			var public_key = new byte[] {};
			
			
			if(memo!=null) {
				private_key = SignumCrypto.getInstance().getPrivateKey(memo);
			}else if(private_key_str != null) {
				private_key = Hex.decode(private_key_str);
			}
			if(public_key_str!=null) {
				public_key = Hex.decode(public_key_str);
			}else {
				public_key = SignumCrypto.getInstance().getPublicKey(private_key);
			}
			if(jobj_arg.optBoolean("show_id")) {
				var id =  SignumCrypto.getInstance().getAddressFromPublic(public_key).getID();
				System.out.println(id);
			}else {
				var address = SignumCrypto.getInstance().getAddressFromPublic(public_key).getFullAddress();				
				System.out.println("T"+address);
			}
		}
	}

	private static final SignumValue toSignumValue(BigDecimal amount) {
		return SignumValue.fromNQT(amount.multiply(BigDecimal.TEN.pow(peth_decimals)).toBigInteger());
	}

}
