package br.com.cmabreu.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.cmabreu.workers.BruteWorker;

@Service
@EnableScheduling
public class BruteService {
	private List<String> words;
	private List<BruteWorker> workers;
	
	@PostConstruct
	public void init() {
		this.workers = new ArrayList<BruteWorker>();
		this.words = new ArrayList<String>();
		new File("/brute/wallets").mkdirs();
		new File("/brute/config").mkdirs();
		try {
			loadWords();
			for( int x=0; x < 500; x++ ) summonWorker();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	@Scheduled(fixedDelay = 1000 * 60 ) 
	private void update() {
		int total = 0;
		for( BruteWorker wk : this.workers ) {
			total = total + wk.getQuant();
		}
		System.out.println("Total Processed: " + total);
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
		BruteWorker worker = new BruteWorker( this.words );
		this.workers.add( worker );
		new Thread( worker ).start();
	}
	

}
