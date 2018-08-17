<?php

	$repoList = explode("\n",file_get_contents("tech-repos.txt"));
	
	$scriptInstance = $argv[1];
	$totalScriptInstances = $argv[2];
	
	for($i = 0; $i < count($repoList); $i++) {
		if($i % $totalScriptInstances != $scriptInstance)
			continue;

		$repoLine = $repoList[$i];
		$found = true;
		echo $repoLine . "\r\n";
		
		$dir = parse_url($repoLine)["path"];
		$dir = str_replace("/", "__", $dir);
		$dir = ltrim($dir, "_");
		$dir = rtrim($dir, ".git");
		if(strlen($dir) == 0)
			continue;

		$dir = "tech-projects/" . urlencode($dir);
		
		$repoLine = str_replace("https://","https://ignore:ignore@", $repoLine);

		if(file_exists($dir))
			continue;

		exec("git clone $repoLine " . $dir);
		if(!file_exists($dir . "/pom.xml")) {
			file_put_contents('removeNonMaven.txt', "rm $dir\n", FILE_APPEND | LOCK_EX);
			//unlink($dir);
		}
	}
	
	if(!$found) {
		echo "error: $goodItem \r\n";
	}

	
	function removeExt($item) {
		return substr($item, 0, strpos($item, "."));
	}
	
	
	function str_lreplace($search, $replace, $subject)
	{
		$pos = strrpos($subject, $search);

		if($pos !== false)
		{
			$subject = substr_replace($subject, $replace, $pos, strlen($search));
		}

		return $subject;
	}
	

//https://github.com/CloudSlang/cloud-slang.git
//tech-projects/CloudSlang-cloud-slang.txt
