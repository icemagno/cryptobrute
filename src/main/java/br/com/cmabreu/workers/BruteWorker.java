package br.com.cmabreu.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
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
	private boolean working = true;
	private List<Wallet> lastWallets = new ArrayList<Wallet>();
	
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
			cWallet.setDumpText( ethGetBalance.getRawResponse() );
			if( this.config.getBoolean("saveAllWallets") || !balance.equals("0") ) {
				cWallet.setBalance(balance);
				cWallet.setNetwork( netName );
				writeWallet( cWallet );
				this.bs.sendWallet(cWallet);
				this.stats.put(netName, this.stats.get( netName ) + 1);
				found++;
				dumpWallets( address );
			}
			success++;
			return res;
		} catch ( Exception e ) {
			errors++;
			return new BigDecimal(0);
		}
	}

	private void dumpWallets( String address ) throws Exception {
		new File("/brute/wallets/" + address ).mkdirs();
		System.out.println("Dumping all previous wallets because found balance on " + address );
		for( Wallet w : this.lastWallets ) {
			System.out.println("   > " + w.getAddress() );
			writeWallet(w, "/brute/wallets/" + address );
		}
		
	}

	public void writeWallet( Wallet wallet, String folder ) throws IOException {
		JSONObject jo = new JSONObject( wallet );
		BufferedWriter writer = new BufferedWriter(new FileWriter( folder + "/" + wallet.getAddress() + ".txt" )  );
		writer.write( jo.toString() );
		writer.close();
	}	
	
	
	public void writeWallet( Wallet wallet ) throws IOException {
		writeWallet( wallet, "/brute/wallets");
	}	
	
	public Map<String, Web3j> getNetworks() {
		return networks;
	}
	
	public Wallet createWallet( String password ) throws Exception {
		this.quant++;
		String mnemonics = this.getMnemonics();
		Credentials credentials = WalletUtils.loadBip39Credentials( password, mnemonics );	
		ECKeyPair keypair = credentials.getEcKeyPair();
		Wallet cWallet = new Wallet(credentials.getAddress(), mnemonics, keypair.getPublicKey().toString(16), keypair.getPrivateKey().toString(16), password );
		for (Map.Entry<String, Web3j> entry : this.networks.entrySet()) {
			balanceEth( cWallet, entry.getKey(), entry.getValue(), cWallet.getAddress() ).toPlainString();
		}
		this.lastWallets.add( cWallet );
		if( this.lastWallets.size() > 10 ) this.lastWallets.remove(0);		
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
	
	public Map<String, Integer> getStats(){
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
				this.networks.put( netName, Web3j.build(new HttpService( netAddress ) )  );
				this.stats.put(netName, 0);
			}
		}
		while( this.working ) {
			try {
				createWallet( "password" );
				Thread.sleep(500);
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		System.out.println("Worker found a Wallet and stopped.");
	}

}
