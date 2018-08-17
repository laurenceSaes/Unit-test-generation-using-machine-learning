<?php

error_reporting(E_ALL);
ini_set("display_errors",1);

// System
$remainReqLeft = 0;

// Settings
$maxPages = 1;
$itemPerPage = 10;

function sleepWhenNoReqLeft() {
	GLOBAL $access_key;
	GLOBAL $remainReqLeft;
	
	if(--$remainReqLeft > 0)
		return;
		
	$url = "https://api.github.com/rate_limit?access_token=$access_key";
	$agent = 'Project Index';
	
	$curl = curl_init();
	curl_setopt($curl, CURLOPT_URL, $url);
	curl_setopt($curl, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
	curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
	curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
	curl_setopt($curl, CURLOPT_USERAGENT, $agent);
	curl_setopt($curl, CURLOPT_HTTPHEADER, array('Authentication: token ' . $access_key));
	$result = curl_exec($curl);
	curl_close($curl);
	$data = json_decode($result);
			
	//When we have left, continue
	$remainReqLeft = $data->resources->search->remaining - 1;
	if($remainReqLeft >= 0) {
		return;
	}
	
	//We do not have req left
	$sleepFor = $data->resources->search->reset - time() + 1;
	echo "Sleeping for $sleepFor\n";
	sleep($sleepFor);
	
	return sleepWhenNoReqLeft();
}

function doRequest($url, $getAllPages = true) {
	GLOBAL $access_key;
	GLOBAL $maxPages;
	GLOBAL $itemPerPage;
	
	$results = array();
	$agent = 'Project Index';
		
	for($page = 1; $page <= $maxPages; $page++) {
		
		sleepWhenNoReqLeft();
		
		$curl = curl_init();
		$req = $url . "&per_page=$itemPerPage&page=$page&access_token=$access_key";
		curl_setopt($curl, CURLOPT_URL, $req);
		curl_setopt($curl, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
		curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
		curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
		curl_setopt($curl, CURLOPT_USERAGENT, $agent);
		curl_setopt($curl, CURLOPT_HTTPHEADER, array('Authentication: token ' . $access_key));
		$result = curl_exec($curl);
		curl_close($curl);
		$data = json_decode($result);
		
		if($getAllPages)
			echo "############### Fetched page $page (".count($results) . "-" . (count($results) + 100)." limit @ " . $data->total_count . " ) ###############\n";
			
		sleep(1);
		
		//Error, refetch!
		if(!isset($data->items)) {
			
			if(strpos($result, "resources do not exist or you do not have permission") !== false)
				return array();
			
			echo "Result: $result for $req";
			echo "Github error, retry in 3 minutes!\n";
			sleep(180);
			$page--;
			continue;
		}
				
		if($getAllPages == false) {
			return $data->items;
		}
		
		if(!isset($data->items) || count($data->items) == 0)
			return $results;
		
		foreach($data->items as $item) {
			$results[] = $item;
		}
	}
	

	return $results;
}

$done = explode("\n",file_get_contents("checked.txt"));
function processRepos($repoName) {
	global $done;

	if(in_array($repoName,$done)) {
		echo "already checked $repoName\n";
		return;
	}
			
	$valid = isgradleMavenTestProject($repoName);
	
	echo $repoName . " = " . ($valid ? "valid" : "invalid") . "\n";
	
	file_put_contents($valid ? "valid.txt" : "invalid.txt", "$repoName\n", FILE_APPEND);
	file_put_contents("checked.txt", "$repoName\n", FILE_APPEND);
	$done[] = $repoName;
}


function isgradleMavenTestProject($repo) {
	$testProject = "https://api.github.com/search/code?q=import%20org.junit.+repo:$repo";
	if( count(doRequest($testProject, false)) == 0)
		return false;

	$mavenProject = "https://api.github.com/search/code?q=junit4+filename:pom.xml+repo:$repo";
	$mavenRequest = doRequest($mavenProject, false);
	foreach($mavenRequest as $req) {
		if(!isset($req->html_url))
			continue;
		

		$depFile=explode("\n", file_get_contents($req->html_url));
		$sawArtifact = false;
		foreach($depFile as $depLine) {
			if($sawArtifact && strpos($depLine,"version</span>&gt;4.") !== false) {
				return true;
			}
			
			if(strpos($depLine,"dependency") !== false)
				$sawArtifact = false;
			
			if(strpos($depLine,"artifactId") !== false && strpos($depLine,"junit") !== false)
				$sawArtifact = true;
		}
	}

	$gradleProject = "https://api.github.com/search/code?q=junit+filename:build.gradle+repo:$repo";
	$gradleRequest = doRequest($gradleProject, false);
	foreach($gradleRequest as $req) {
		if(!isset($req->html_url))
			continue;

		$depFile=file_get_contents($req->html_url);
		if(strpos($depFile, "android") !== false)
			break;
		if(strpos($depFile, "junit:junit:4") !== false)
			return true;
	}

	return false;
}

$handle = fopen("allJava.txt", "r");
$instance = (int)$argv[1];
$max = (int)$argv[2];
$access_key = $argv[3]; //"APIKEY";

if ($handle) {
    for ($counter = 0; ($line = fgets($handle)) !== false; $counter++) {
	if($counter % $max != $instance)
		continue;

	processRepos(str_replace(array("\n", "\r"), array("",""), $line));
    }

    fclose($handle);
}

