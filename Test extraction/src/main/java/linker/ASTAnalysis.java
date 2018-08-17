package linker;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.CollectionContext;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import linker.methods.ASTCalls;
import logger.LinkerLogger;
import slicer.ClassPathHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ASTAnalysis {

    private final JavaParserFacade javaParserFacade;

    private final List<String> projectFolders;

    private static HashMap<Path, CompilationUnit> compilationUnitCache = new HashMap<>();

    public ASTAnalysis(List<String> projectFolders, List<String> classPaths) {
        javaParserFacade = JavaParserFacade.get(createTypeResolver(classPaths, projectFolders));
        this.projectFolders = projectFolders;
    }

    private List<CompilationUnit> createCompilationUnit(String className) throws IOException {
        List<CompilationUnit> compilationUnits = new ArrayList<>();
        Set<Path> pathsToAnalyze = new HashSet<>();
        for(String projectFolder : projectFolders) {
            Files.walk(Paths.get(projectFolder))
                    .filter(path -> {
                        if (!Files.isRegularFile(path))
                            return false;
                        String fileName = path.getFileName().toString().replaceFirst("[.][^.]+$", "");
                        return className.endsWith("." + fileName);
                    })
                    .forEach(pathsToAnalyze::add);
        }

        for (Path javaClass : pathsToAnalyze) {

            try {
                CompilationUnit compilationUnit = compilationUnitCache.get(javaClass);
                if(compilationUnit == null) {
                    if(compilationUnitCache.size() >= 50) {
                        compilationUnitCache = new HashMap<>();
                    }

                    compilationUnit = JavaParser.parse(javaClass);
                    compilationUnitCache.put(javaClass, compilationUnit);
                }

                compilationUnits.add(compilationUnit);

            } catch (ParseProblemException ex) {

            }
        }

        return compilationUnits;
    }

    private CombinedTypeSolver createTypeResolver(List<String> classPaths, List<String> rootFolders) {
        CombinedTypeSolver typeSolver;
        typeSolver = new CombinedTypeSolver();

        ReflectionTypeSolver refTypeResolver = new ReflectionTypeSolver(false);
        typeSolver.add(refTypeResolver);

        for(String path : classPaths) {
            String resolvedPath = ClassPathHelper.resolvePathAndFixMaven(path);
            if(resolvedPath == null || resolvedPath.isEmpty() || !(new File(resolvedPath).exists()))
                continue;

            if(resolvedPath.endsWith(".src.jar")) {
                try {
                    typeSolver.add(new JarTypeSolver(resolvedPath));
                } catch (IOException e) {
                    LinkerLogger.logError("Cannot open: " + resolvedPath);
                }
            } else if(new File(resolvedPath).isDirectory()){
                typeSolver.add(new JavaParserTypeSolver(new File(resolvedPath)));
            }
        }

        for(String rootFolder : rootFolders) {
            ProjectRoot projectRoot = new CollectionContext(new ParserCollectionStrategy()).collect(Paths.get(rootFolder));

            File rootDirectory;
            for (SourceRoot s : projectRoot.getSourceRoots()) {
                rootDirectory = s.getRoot().toFile();
                TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(rootDirectory);
                typeSolver.add(javaParserTypeSolver);
            }
        }

        return typeSolver;
    }

    public List<JavaMethodReference> findCallsIn(JavaMethodReference testMethod) throws IOException {

        List<CompilationUnit> compilationUnits = createCompilationUnit(testMethod.getClassName());
        if(compilationUnits.size() == 0) {
            LinkerLogger.logWarning("Cannot find complication unit for " + testMethod.getClassName());
        }

        for(CompilationUnit compilationUnit : compilationUnits) {
            Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
            String packageName = "";
            if(packageDeclaration.isPresent()) {
                packageName = packageDeclaration.get().getName().toString();
            }

            ASTCalls astCalls = new ASTCalls(packageName, testMethod, javaParserFacade);
            compilationUnit.accept(astCalls, null);
            if(astCalls.isWasForThisClass()) {
                return astCalls.getResult();
            }
        }

        return new ArrayList<>();

    }
}
