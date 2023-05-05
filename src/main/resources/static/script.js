var provider = null;

$( document ).ready(function() {


	const ws2 = new SockJS( '/ws' );
	var stompClient = Stomp.over(ws2);
    stompClient.debug = () => {};
    stompClient.connect({}, (frame) => {
    	
      	stompClient.subscribe('/wallets', (message) => {
      		if(message.body) {
      			addWallet(  JSON.parse(message.body)  );
      		}
      	});

      	stompClient.subscribe('/statistic', (message) => {
      		if(message.body) {
      			createUpdateNetStatistic( JSON.parse(message.body));
      		}
      	});
      	
      	
	});
	
	provider = new ethers.providers.JsonRpcProvider("https://bsc-dataseed1.binance.org/");
});

async function  verifyWallet( wallet, network ){
	balance = await provider.getBalance("0xd0438D4539867cC3b58f0ce6824bEe58787c70Bd", "latest"); // 0.0045
	// balance = await provider.getBalance( wallet );
	var bb = ethers.utils.formatEther(balance);
	
    $(document).Toasts('create', {
        class: 'bg-info',
        title: wallet,
        subtitle: '',
        body: 'Balance is ' + bb
	})	
	
}

function addWallet( payload ){
	if( $( "#wallets tr").length > 10 )	$('#wallets tr:first').remove();
	let el = "<tr>" +
	"<td>"+ payload.address +"</td>" +
	"<td>" + payload.balance +"</td>" +
	"<td>" + payload.network +"</td>" +
	"<td><button type='button' onClick='verifyWallet(\""+ payload.address+ "\",\""+ payload.network +"\")'  class='btn btn-outline-info btn-xs'>Verify</button></td>" +
	"</tr>";
	$("#wallets").append( el );	
	
}

function createUpdateNetStatistic( payload ){
	Object.keys( payload.statistic ).forEach(function(key) {
		  const hash = cyrb53(key);
		  const divId = "_" + hash;
		  if ( $( "#" + divId ).length ) {
			  $( "#" + divId ).text( payload.statistic[key] );
		  } else {
				let el = "<tr>" +
				"<td>"+ key +"</td>" +
				"<td id='"+divId+"'>" + payload.statistic[key] +"</td>" +
				"</tr>";
			$("#netcount").append( el );			  
		  }
	}); 
	
	$( "#stat_success" ).text( payload.success );
	$( "#stat_error" ).text( payload.errors );
	$( "#stat_found" ).text( payload.found );
	$( "#stat_total" ).text( payload.total );
	
}


const cyrb53 = (str, seed = 0) => {
    let h1 = 0xdeadbeef ^ seed, h2 = 0x41c6ce57 ^ seed;
    for(let i = 0, ch; i < str.length; i++) {
        ch = str.charCodeAt(i);
        h1 = Math.imul(h1 ^ ch, 2654435761);
        h2 = Math.imul(h2 ^ ch, 1597334677);
    }
    h1  = Math.imul(h1 ^ (h1 >>> 16), 2246822507);
    h1 ^= Math.imul(h2 ^ (h2 >>> 13), 3266489909);
    h2  = Math.imul(h2 ^ (h2 >>> 16), 2246822507);
    h2 ^= Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  
    return 4294967296 * (2097151 & h2) + (h1 >>> 0);
};

