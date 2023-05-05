package br.com.cmabreu.model;

import java.util.ArrayList;
import java.util.List;

public class Wallet {
	private String address;
	private String mnemonic;
	private String pubk;
	private String privk;
	private String balance;
	private String network;
	private String password;
	private List<String> dumpText = new ArrayList<String>();
	
	public Wallet(String address, String mnemonic, String pubk, String privk, String password ) {
		this.address = address;
		this.mnemonic = mnemonic;
		this.pubk = pubk;
		this.privk = privk;
		this.network = "";
		this.balance = "0";
		this.password = password;
	}
	
	public void setDumpText(String dumpText) {
		this.dumpText.add( dumpText );
	}
	
	public List<String> getDumpText() {
		return dumpText;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setNetwork(String network) {
		this.network = network;
	}
	
	public String getNetwork() {
		return network;
	}
	
	public void setBalance(String balance) {
		this.balance = balance;
	}
	
	public String getBalance() {
		return balance;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getMnemonic() {
		return mnemonic;
	}
	public void setMnemonic(String mnemonic) {
		this.mnemonic = mnemonic;
	}
	public String getPubk() {
		return pubk;
	}
	public void setPubk(String pubk) {
		this.pubk = pubk;
	}
	public String getPrivk() {
		return privk;
	}
	public void setPrivk(String privk) {
		this.privk = privk;
	}
	
}
