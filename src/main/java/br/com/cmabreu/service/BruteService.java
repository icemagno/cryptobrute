package br.com.cmabreu.service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

import br.com.cmabreu.model.Wallet;
import br.com.cmabreu.workers.BruteWorker;

@Service
@EnableScheduling
public class BruteService {
	private List<String> words;
	private List<BruteWorker> workers;
	private JSONObject config;
	private Map<String, Integer> stats;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	public synchronized void sendWallet( Wallet wallet ) {
		messagingTemplate.convertAndSend("/wallets", wallet );
	}	
	
    @Scheduled( fixedDelay = 1000 * 5 )
    private void ping() {
    	messagingTemplate.convertAndSend("/ping", "PING" );
    }
    	
	
	@PostConstruct
	public void init() {
		this.stats = new HashMap<String, Integer>();
		this.workers = new ArrayList<BruteWorker>();
		this.words = new ArrayList<String>();
		new File("/brute/wallets").mkdirs();
		new File("/brute/config").mkdirs();
		try {
			loadWords();
			loadConfig();
			for( int x=0; x < this.config.getInt("workers"); x++ ) summonWorker();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	@Scheduled(fixedDelay = 1000 * 10 ) 
	private void update() {
		int total = 0;
		int errors = 0;
		int found = 0;
		int success = 0;
		JSONObject statistics = new JSONObject();
		for( BruteWorker wk : this.workers ) {
			total = total + wk.getQuant();
			errors = errors + wk.getErrors();
			found = found + wk.getFound();
			success = success + wk.getSuccess();
			for (Map.Entry<String, Integer> entry : wk.getStats().entrySet()) {
				int totalPartial = this.stats.get( entry.getKey() );
				this.stats.put( entry.getKey(), totalPartial + entry.getValue() ) ;
				statistics.put( entry.getKey(), this.stats.get( entry.getKey() ) );
			}
		}
		JSONObject statisticData = new JSONObject();
		statisticData.put("total", total);
		statisticData.put("errors", errors);
		statisticData.put("found", found);
		statisticData.put("success", success);
		statisticData.put("statistic", statistics);
		messagingTemplate.convertAndSend("/statistic", statisticData.toString() );
	}
	
	private void loadConfig() throws Exception {
		byte[] encoded = Files.readAllBytes(Paths.get("/brute/config/config.json"));
		this.config = new JSONObject( new String(encoded, StandardCharsets.UTF_8) );
		
		JSONArray nets = this.config.getJSONArray("networks");
		for ( int x=0; x < nets.length(); x++ ) {
			JSONObject net = nets.getJSONObject(x);
			if ( net.getBoolean("active") ) {
				this.stats.put( net.getString("name"), 0);
			}
		}
		
	}
	
	private void loadWords() throws Exception {
		Scanner scanner = new Scanner( new File("/brute/config/words.txt") );
		while (scanner.hasNextLine()) {
			String word = scanner.nextLine();
			this.words.add( word );
		}
		scanner.close();		
	}	
	
	private void summonWorker() {
		BruteWorker worker = new BruteWorker( this );
		this.workers.add( worker );
		new Thread( worker ).start();
	}
	
	public JSONObject getConfig() {
		return config;
	}
	
	public List<String> getWords() {
		return words;
	}

	public String reCheck(String addr) {
		JSONObject result = new JSONObject();
		result.put("address", addr);
		for (Map.Entry<String, Web3j> entry : this.workers.get(0).getNetworks().entrySet()) {
			try {
				EthGetBalance ethGetBalance = entry.getValue().ethGetBalance(addr, DefaultBlockParameterName.LATEST).send();
				BigDecimal res = Convert.fromWei( ethGetBalance.getBalance().toString() , Unit.ETHER );
				String balance = res.toPlainString();
				result.put( entry.getKey(), balance);
			} catch ( Exception e ) {
				//
			}
		}
		return result.toString();
	}
	
}
