<!DOCTYPE html>
<html>
<head>
<script>
	function init(){
		if (navigator.userAgent.indexOf('Trident') > 0){
			window.location = 'microsoft-edge:' + 'https:dstamp.kosaf.go.kr'
			setTimeout(function (){
				window.open('','_self').close()
			}, 1)
		} else {
			window.location = 'https://dstamp.kosaf.go.kr'
		}
	}
</script>
</head>
<body onload="init()">
</body>
</html>