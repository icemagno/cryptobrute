package br.com.cmabreu.workers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

import br.com.cmabreu.model.Wallet;

public class BruteWorker implements Runnable {
	private List<String> words;
	private Map<String, Web3j> networks;
	private int quant = 0;
	
	public BruteWorker(List<String> words) {
		this.words = words;
	}
	
	private synchronized BigDecimal balanceEth(Wallet cWallet, String netName, Web3j network, String address) {
		try {
			EthGetBalance ethGetBalance = network.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
			BigDecimal res = Convert.fromWei( ethGetBalance.getBalance().toString() , Unit.ETHER );
			String balance = res.toPlainString();
			if( !balance.equals("0") ) {
				System.out.println(netName + " > " + address + " " + balance );
				cWallet.setBalance(balance);
				cWallet.setNetwork( netName );
				writeWallet( cWallet );
			}
			
			return res;
		} catch ( Exception e ) {
			// System.out.println(netName + " > " + e.getMessage() );
			return new BigDecimal(0);
		}
	}

	public void writeWallet( Wallet wallet ) throws IOException {
		JSONObject jo = new JSONObject( wallet );
		BufferedWriter writer = new BufferedWriter(new FileWriter("/brute/wallets/" + wallet.getAddress() + ".txt" )  );
		writer.write( jo.toString() );
		writer.close();
	}	
	
	public Wallet createWallet( String password ) throws Exception {
		String mnemonics = this.getMnemonics();
		Credentials credentials = WalletUtils.loadBip39Credentials( password, mnemonics );	
		ECKeyPair keypair = credentials.getEcKeyPair();
		Wallet cWallet = new Wallet(credentials.getAddress(), mnemonics, keypair.getPublicKey().toString(16), keypair.getPrivateKey().toString(16), password );
		for (Map.Entry<String, Web3j> entry : this.networks.entrySet()) {
			balanceEth( cWallet, entry.getKey(), entry.getValue(), cWallet.getAddress() ).toPlainString();
		}
		return cWallet;
	}
	
	private String getMnemonics() {
		Random r = new Random();
		StringBuilder result = new StringBuilder();
		for( int x=0; x < 12; x++ ) {
			String aWord = this.words.get( r.nextInt( this.words.size() ) );
			result.append( aWord );
			result.append(" ");
		}
		return result.toString().trim();
	}
	
	public int getQuant() {
		int temp = quant;
		quant = 0;
		return temp;
	}
	
	@Override
	public void run() {
		this.networks = new HashMap<String, Web3j>();
		this.networks.put("Binance BSC", Web3j.build(new HttpService( "https://bsc-dataseed.binance.org" ) ) );
		this.networks.put("MATIC Polygon", Web3j.build(new HttpService( "https://polygon-rpc.com" ) ) );
		this.networks.put("MATIC Polygon Llama", Web3j.build(new HttpService( "https://polygon.llamarpc.com" ) ) );
		this.networks.put("Ethereum", Web3j.build(new HttpService( "https://mainnet.infura.io/v3/e8ce20ba5fde4c2b8580c69e4517f021" ) ) );
		this.networks.put("AVAX Avalanche", Web3j.build(new HttpService( "https://api.avax.network/ext/bc/C/rpc" ) ) );
		while( true ) {
			try {
				createWallet( "password" );
				quant++;
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

}
