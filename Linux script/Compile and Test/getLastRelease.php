<?php

error_reporting(E_ALL);
ini_set("display_errors",0);

// System
$remainReqLeft = 0;

// Settings
$maxPages = 1;
$itemPerPage = 10;

$access_key = "APIKEY";

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
	sleep($sleepFor);
	
	return sleepWhenNoReqLeft();
}

function doRequest($url, $getAllPages = true) {
	GLOBAL $access_key;
	GLOBAL $maxPages;
	GLOBAL $itemPerPage;
	
	$results = array();
	$agent = 'Project Index';

	sleepWhenNoReqLeft();
	
	$curl = curl_init();
	$req = $url . "?access_token=$access_key";

	curl_setopt($curl, CURLOPT_URL, $req);
	curl_setopt($curl, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));
	curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
	curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
	curl_setopt($curl, CURLOPT_USERAGENT, $agent);
	curl_setopt($curl, CURLOPT_HTTPHEADER, array('Authentication: token ' . $access_key));
	$result = curl_exec($curl);
	curl_close($curl);
	$data = json_decode($result);
	
	return $data;		
}

$project = $argv[1];
$project = explode("__", $project);
if(count($project) != 2)
	die("Wrong input");

$link = "https://api.github.com/repos/$project[0]/$project[1]/releases/latest";
$result = doRequest($link, false);

if(isset($result->tag_name)) {
	echo $result->tag_name;
}