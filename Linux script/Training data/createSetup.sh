#!/bin/bash
fromProjects=9999;
inputSizes=(50);
modes=(mixed normal sbt)
compresions=(0 2 5 10)
database="unittests"
outputDir="Data"

artifactDir=$PWD
rm -r $artifactDir/$outputDir/*

renameForGoogle () {
   location=$1
   compressLevel=$2
   refLocation=$3
   dev="$location/dev/"
   test="$location/test/"
   train="$location/train/"

   mkdir "$dev"
   mkdir "$test"
   mkdir "$train"

   if [ -f "$location/compress.dict.vocab" ]
   then
      cp "$location/compress.dict.vocab" "$train/vocab.sources.txt"
      cp "$location/compress.dict.vocab" "$train/vocab.targets.txt"
   fi

   cp "$location/src-train.txt" "$train/sources.txt"
   cp "$location/targ-train.txt" "$train/targets.txt"

   cp "$location/src-val.txt" "$dev/sources.txt"
   cp "$location/targ-val.txt" "$dev/targets.txt"

   cp "$location/src-eval.txt" "$test/sources.txt"
   cp "$location/targ-eval.txt" "$test/targets.txt"

   if [ "$compressLevel" -eq "0" ]
   then
	bpeLocation="$location-BPE"
	devBPE="$bpeLocation/dev/"
        testBPE="$bpeLocation/test/"  
        trainBPE="$bpeLocation/train/"

	mkdir -p $devBPE
	mkdir -p $testBPE
	mkdir -p $trainBPE

#Exceptions. Strage. Use debugger

	$(cd $dev && java -jar $artifactDir/BPE_tokens/main.jar "sources.txt;targets.txt" "../compress.dict")
	mv $dev/sources.txt.bpe $devBPE/sources.txt
	mv $dev/targets.txt.bpe $devBPE/targets.txt
	cp $refLocation/targ-val.txt $devBPE/targetsRef.txt

	$(cd $test && java -jar $artifactDir/BPE_tokens/main.jar "sources.txt;targets.txt" "../compress.dict")
	mv $test/sources.txt.bpe $testBPE/sources.txt
	mv $test/targets.txt.bpe $testBPE/targets.txt

	$(cd $train && java -jar $artifactDir/BPE_tokens/main.jar "sources.txt;targets.txt" "../compress.dict")
	mv $train/sources.txt.bpe $trainBPE/sources.txt
	mv $train/targets.txt.bpe $trainBPE/targets.txt

        cp $train/../compress.dict.bpe $trainBPE/vocab.sources.txt
        mv $train/../compress.dict.bpe $trainBPE/vocab.targets.txt

   fi
}


for mode in ${modes[*]}
do
	for inputSize in ${inputSizes[*]}
	do
		soreLocation="$outputDir/size-$inputSize-mode-$mode"
		soreLocationTokens="$soreLocation-token"

		java -jar Training/main.jar $inputSize $mode $database
		mkdir $soreLocation
		cp *.txt "$soreLocation/."

		for compressTimes in ${compresions[*]}
		do
			compressLocation="$soreLocationTokens-compress-$compressTimes"
			mkdir $compressLocation

			$(cd $soreLocation && java -jar $artifactDir/MultiToken/main.jar "src-train.txt;targ-train.txt;src-val.txt;targ-val.txt;src-eval.txt;targ-eval.txt" "compress.dict" "$compressTimes"  )

			mv $soreLocation/*.multi.token "$compressLocation/."
			mv $soreLocation/compress.dict "$compressLocation/."
			mv $soreLocation/compress.dict.tab "$compressLocation/."
			mv $soreLocation/compress.dict.vocab "$compressLocation/."
			mv $soreLocation/compress.dict.bypass.tab "$compressLocation/."

			rename 's/\.multi\.token//' $compressLocation/*.multi.token

			renameForGoogle "$compressLocation" "$compressTimes" "$soreLocation"
		done
		renameForGoogle "$soreLocation" "-1"
	done
done

