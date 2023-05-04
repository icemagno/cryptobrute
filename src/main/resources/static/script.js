$( document ).ready(function() {


	const ws2 = new SockJS( '/ws' );
	var stompClient = Stomp.over(ws2);
    stompClient.debug = () => {};
    stompClient.connect({}, (frame) => {
    	
      	stompClient.subscribe('/wallets', (message) => {
      		if(message.body) {
      			console.log(  JSON.parse(message.body)  );
      		}
      	});

      	stompClient.subscribe('/statistic', (message) => {
      		if(message.body) {
      			console.log(  JSON.parse(message.body)  );
      		}
      	});
      	
      	
	});
	
	
	
});