bash statsAST.sh
bash linkProgress.sh unittests_ast
#printf "projects analyzed: "
#bash logLinkMemory.sh "AST" | wc -l
ps aux | grep AST | grep main.jar
ls -tl log/link/AST_MEM_BYTECODE_*_*_0_*_output.log | head -1
ls -tl log/link/AST_MEM_BYTECODE_*_*_1_*_output.log | head -1
ls -tl log/link/AST_MEM_BYTECODE_*_*_2_*_output.log | head -1
printf "Training data amount:"
bash trainingDataAmount.sh unittests_ast

