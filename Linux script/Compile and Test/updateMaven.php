
<?php

        $folders = array_values(array_filter(glob('tech-projects/*'), 'is_dir'));
        $scriptInstance = $argv[1];
        $totalScriptInstances = $argv[2];

        for($i = 0; $i < count($folders); $i++) {
                if($i % $totalScriptInstances != $scriptInstance)
                        continue;

                echo "($i) $folder\n";
                execute($folder, "mvn dependency:resolve");
        }

        function execute($folder, $command) {
                return shell_exec("cd $folder && $command");
        }



