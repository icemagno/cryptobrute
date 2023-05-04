package br.com.cmabreu.workers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
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
import br.com.cmabreu.service.BruteService;

public class BruteWorker implements Runnable {
	private List<String> words;
	private Map<String, Web3j> networks;
	private Map<String, Integer> stats;
	private int quant = 0;
	private int success = 0;
	private int errors = 0;
	private int found = 0;
	private JSONObject config;
	private BruteService bs;
	
	public BruteWorker( BruteService bs) {
		this.words = bs.getWords();
		this.config = bs.getConfig();
		this.bs = bs;
	}
	
	private synchronized BigDecimal balanceEth(Wallet cWallet, String netName, Web3j network, String address) {
		try {
			EthGetBalance ethGetBalance = network.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
			BigDecimal res = Convert.fromWei( ethGetBalance.getBalance().toString() , Unit.ETHER );
			String balance = res.toPlainString();
			if( this.config.getBoolean("saveAllWallets") || !balance.equals("0") ) {
				System.out.println(netName + " > " + address + " " + balance );
				cWallet.setBalance(balance);
				cWallet.setNetwork( netName );
				writeWallet( cWallet );
				this.bs.sendWallet(cWallet);
				this.stats.put(netName, this.stats.get( netName ) + 1);
				found++;
			}
			success++;
			return res;
		} catch ( Exception e ) {
			errors++;
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
			Thread.sleep(500);
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
	
	public int getFound() {
		return found;
	}
	
	public int getQuant() {
		return quant;
	}
	
	public int getErrors() {
		return errors;
	}

	public int getSuccess() {
		return success;
	}
	
	public synchronized  Map<String, Integer> getStats(){
		return this.stats;
	}
	
	@Override
	public void run() {
		this.networks = new HashMap<String, Web3j>();
		this.stats = new HashMap<String, Integer>();
		JSONArray nets = this.config.getJSONArray("networks");
		for ( int x=0; x < nets.length(); x++ ) {
			JSONObject net = nets.getJSONObject(x);
			if ( net.getBoolean("active") ) {
				String netName = net.getString("name");
				String netAddress = net.getString("apiAddress");
				System.out.println("  > " + netName + " - " + netAddress );
				this.networks.put( netName, Web3j.build(new HttpService( netAddress ) )  );
				this.stats.put(netName, 0);
			}
		}
		while( true ) {
			try {
				createWallet( "password" );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

}
