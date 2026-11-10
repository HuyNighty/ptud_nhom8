package com.quanlynhakhoa.architecture;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Parses Java syntax to enforce layer boundaries; no application or database execution. */
public final class LayeredArchitectureSelfTest {
    private static int checks;
    private static void check(boolean ok, String why) { checks++; if(!ok)throw new AssertionError(why); }
    public static void main(String[] args)throws Exception {
        Path root=Path.of("src/main/java").toAbsolutePath();
        Set<String> jdbc=Set.of("prepareStatement","prepareCall","createStatement");
        Set<String> transactions=Set.of("commit","rollback","setAutoCommit","setTransactionIsolation");
        Set<String> callerOwned=Set.of("BaoCaoDoanhThuDao.java","PhiLamSangDao.java","TaoHoaDonDao.java",
                "LanThanhToanDao.java","DaoThanhToanDao.java","PhanQuyenCoBanDao.java");
        Set<String> businessFields=Set.of("invoiceId","patientId","accountId","paymentId","employeeId","selectedDoctorId");
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();check(compiler!=null,"Run architecture checks on a JDK");
        List<Path> paths;try(var stream=Files.walk(root)){paths=stream.filter(p->p.toString().endsWith(".java")).toList();}
        try(StandardJavaFileManager files=compiler.getStandardFileManager(null,null,java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task=(JavacTask)compiler.getTask(null,files,null,List.of("-proc:none","--release","21"),null,files.getJavaFileObjectsFromPaths(paths));
            for(CompilationUnitTree unit:task.parse()) {
                String path=root.relativize(Path.of(unit.getSourceFile().toUri())).toString().replace('\\','/');
                boolean orchestration=path.contains("/service/")||path.contains("/controller/")||path.contains("/view/");
                boolean dao=path.contains("/dao/")&&callerOwned.contains(Path.of(path).getFileName().toString());
                new TreePathScanner<Void,Void>() {
                    public Void visitImport(ImportTree node,Void unused) {
                        if(path.contains("/dao/")) {
                            String imported=node.getQualifiedIdentifier().toString();
                            check(!imported.startsWith("com.quanlynhakhoa.service.")
                                            && !imported.startsWith("com.quanlynhakhoa.controller.")
                                            && !imported.startsWith("com.quanlynhakhoa.view."),
                                    "DAO depends on an orchestration layer: "+path+" / "+imported);
                        }
                        return super.visitImport(node,unused);
                    }
                    public Void visitMethodInvocation(MethodInvocationTree node,Void unused) {
                        String name=node.getMethodSelect() instanceof MemberSelectTree member
                                ?member.getIdentifier().toString():node.getMethodSelect().toString();
                        if(orchestration)check(!jdbc.contains(name),"Direct JDBC statement outside DAO: "+path+" / "+name);
                        if(dao)check(!transactions.contains(name),"DAO must preserve caller-owned transaction: "+path+" / "+name);
                        return super.visitMethodInvocation(node,unused);
                    }
                    public Void visitLiteral(LiteralTree node,Void unused) {
                        if(orchestration && node.getValue() instanceof String text)
                            check(!text.stripLeading().matches("(?is)(SELECT|WITH|INSERT|UPDATE|DELETE|ALTER|CREATE|DROP|EXEC|GRANT|REVOKE)\\s+.*"),
                                    "SQL literal outside DAO: "+path);
                        return super.visitLiteral(node,unused);
                    }
                    public Void visitVariable(VariableTree node,Void unused) {
                        if(getCurrentPath().getParentPath().getLeaf() instanceof ClassTree)
                            check(!businessFields.contains(node.getName().toString()),"Untranslated business identity field: "+path+" / "+node.getName());
                        return super.visitVariable(node,unused);
                    }
                }.scan(unit,null);
            }
        }
        System.out.println("PASS LayeredArchitectureSelfTest: "+checks+" syntax checks; JDBC stays in DAO, transaction ownership and business identity names.");
    }
}
