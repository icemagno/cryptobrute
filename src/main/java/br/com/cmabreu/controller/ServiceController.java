package br.com.cmabreu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.cmabreu.service.BruteService;

@RestController
public class ServiceController {

	@Autowired
	private BruteService bs;
    
	@GetMapping(value = "/recheck", produces=MediaType.APPLICATION_JSON_VALUE )
	public String setFov(  @RequestParam(value="addr",required=true) String addr ) {
		return bs.reCheck( addr );
	}		
	
}
