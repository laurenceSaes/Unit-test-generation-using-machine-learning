<?php 

	$folders = array_values(array_filter(glob('tech-projects/*'), 'is_dir'));
	$scriptInstance = $argv[1];
	$totalScriptInstances = $argv[2];
	
	for($i = 0; $i < count($folders); $i++) {
		if($i % $totalScriptInstances != $scriptInstance)
			continue;
		$folder = $folders[$i];

		if(file_exists($folder . "/report.txt")) {
			echo "$folder is already analyzed!\n";
			continue;
		}

		echo "working on $i ($folder)\n";
		
		testProject($folder);
		echo "Done with: $folder\n";
	}
	
	function testProject($folder) {
		echo "Get last tag\n";
		$tagName = execute($folder, "git describe --abbrev=0 --tags") . "\n";

		if(/*!file_exists($folder . "/tag.txt") &&*/ strpos($tagName, 'fatal:') === false && strpos($tagName,  "Try --always") === false)
		{
			file_put_contents($folder . "/tag.txt", $tagName);
			echo "Checkout\n";
			execute($folder, "git checkout \"$tagName\" >> /dev/null");
		}
		
		$start = time();

		if(file_exists($folder . "/pom.xml")) {
			doMaven($folder);
		} else if(file_exists($folder . "/build.gradle")) {
			return; //skip
			doGradle($folder);	
		} else {
			echo "Error, $folder is not maven or gradle\n";
			return;
		}
		
	    $doneFile = $folder . "/" . "cpDone.txt";
		file_put_contents($doneFile,"yes");
	}
	
	function doGradle($folder) {
		
		//echo "Compile\n";
		//execute($folder, "gradle build");
		
		if(!file_exists($folder . "/cp.txt")) {
			echo "Get class path\n";
			$extract = "task writeClasspath << {
							buildDir.mkdirs()
							new File(file('.').absolutePath, \"cp.txt\").text = configurations.runtime.asPath + \"\\n\"
						}";
						
			$gradleFile = $folder . "/build.gradle";
			$gradleContent = file_get_contents($gradleFile);
			if( strpos($gradleContent, "task writeClasspath") === false) {
				file_put_contents($gradleFile, "\n" . $extract, FILE_APPEND);
			}
			execute($folder, "gradle writeClasspath");
		}
		
		echo "Test\n";
		execute($folder, "gradle test > reportTmp.txt ");
		file_put_contents("$folder/report.txt", file_get_contents("$folder/reportTmp.txt"));
	}
	
	function doMaven($folder) {
		if(!file_exists($folder . "/cpDone.txt")) {
			echo "Compile and get class path\n";
			$compileLog = executeOutput($folder, "mvn dependency:build-classpath -Dmdep.outputFile=cp.txt");
		}
		
		echo "Test\n";
		$testReport = execute($folder, "mvn test --fae -Dmaven.test.failure.ignore=true -Dlicense.skip=true -Dcheckstyle.skip > reportTmp.txt ");
		file_put_contents("$folder/report.txt", file_get_contents("$folder/reportTmp.txt"));
	}

	function executeOutput($folder, $command) {
			return shell_exec("cd $folder && $command");
	}
	
	function execute($folder, $command) {
		return shell_exec("cd $folder && $command 2>&1");
	}	
	
	function executeWithReturn($folder, $command, &$returnCode) {
		$returnText = shell_exec("cd $folder && $command 2>&1 && echo \"|%errorlevel%\"");	
		$returnCode = (int)substr($returnText, strrpos($returnText, '|') + 1);
		return substr($returnText, 0, strrpos($returnText, '|') + 1);
	}	
