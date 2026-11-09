package com.quanlynhakhoa.dao.vaitro;

import com.quanlynhakhoa.entity.VaiTro;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/** Static source/seed contracts only. Does not parse/execute T-SQL or open JDBC. */
public final class RbacV9SqlContractSelfTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;
        if(!ok) throw new AssertionError(message);
    }
    private static String section(String sql,String name) {
        String start="-- BEGIN "+name,end="-- END "+name;
        check(sql.indexOf(start)>=0&&sql.indexOf(end)>sql.indexOf(start),"Missing section "+name);
        return sql.substring(sql.indexOf(start)+start.length(),sql.indexOf(end));
    }
    private static List<List<String>> rows(String sql,String name,String regex,int columns) {
        Matcher matcher=Pattern.compile(regex).matcher(section(sql,name));
        List<List<String>> result=new ArrayList<>();
        while(matcher.find()) {
            List<String> row=new ArrayList<>();
            for(int i=1;i<=columns;i++)row.add(matcher.group(i));
            result.add(row);
        }
        return result;
    }
    private static Set<String> expectedGrants() {
        Set<String> pairs=new TreeSet<>();RolePermissionPolicy policy=new RolePermissionPolicy();
        for(VaiTroHeThong role:VaiTroHeThong.values())for(Quyen permission:policy.permissionsFor(role))
            pairs.add(role.name()+":"+permission.name());
        return pairs;
    }
    private static Set<String> pairs(List<List<String>> rows) {
        return rows.stream().map(r->r.get(0)+":"+r.get(1)).collect(Collectors.toSet());
    }
    private static String noComments(String sql) {
        return sql.replaceAll("(?m)--[^\\r\\n]*","");
    }
    private static void has(String source,String text) { check(source.contains(text),"Missing contract: "+text); }
    public static void main(String[] args)throws Exception {
        Path root=args.length==0?Path.of(""):Path.of(args[0]);
        Path bootstrap=root.resolve("sql/bootstrap");
        String sql=Files.readString(bootstrap.resolve("V9__create_dynamic_rbac.sql"));
        String code=noComments(sql);
        List<List<String>> roles=rows(sql,"SEED ROLES","\\('([A-Z_]+)',N'([^']*)',N'([^']*)',([01]),([01]),([01])\\)",6);
        List<List<String>> permissions=rows(sql,"SEED PERMISSIONS","\\('([A-Z_]+)',N'([^']*)',([01])\\)",3);
        List<List<String>> grants=rows(sql,"SEED GRANTS","\\('([A-Z_]+)','([A-Z_]+)'\\)",2);
        check(VaiTroHeThong.values().length==6&&Quyen.values().length==26,"Baseline enum contract changed");
        check(roles.size()==6,"Exactly six role seed rows");
        Set<String> actualRoles=new HashSet<>();
        for(List<String> row:roles) {
            check(actualRoles.add(row.get(0)),"Duplicate role seed");
            VaiTroHeThong.valueOf(row.get(0));
            VaiTro entity=new VaiTro(row.get(0),row.get(1),row.get(2),row.get(3).equals("1"),
                    row.get(4).equals("1"),row.get(5).equals("1"),new byte[8]);
            check(entity.active()&&entity.system(),"Legacy seed active/system");
            check(entity.protectedRole()==(entity.code().equals("SYSTEM_ADMIN")),"Protected role parity");
        }
        check(actualRoles.equals(Arrays.stream(VaiTroHeThong.values()).map(Enum::name).collect(Collectors.toSet())),"Exact role set");
        check(permissions.size()==26,"Exactly 26 permission seed rows");
        Set<String> actualPermissions=new HashSet<>();
        for(List<String> row:permissions) {
            check(actualPermissions.add(row.get(0)),"Duplicate permission seed");
            Quyen.valueOf(row.get(0));check(!row.get(1).isBlank()&&row.get(2).equals("1"),"Enabled/named permission");
        }
        check(actualPermissions.equals(Arrays.stream(Quyen.values()).map(Enum::name).collect(Collectors.toSet())),"Exact permission set");
        Set<String> actualGrants=pairs(grants),expected=expectedGrants();
        check(expected.size()==63,"Baseline grant count drift");
        check(grants.size()==63&&actualGrants.size()==grants.size(),"63 unique grants");
        check(actualGrants.equals(expected),"Exact role/permission policy parity (both directions)");
        RolePermissionPolicy policy=new RolePermissionPolicy();
        for(VaiTroHeThong role:VaiTroHeThong.values())for(Quyen permission:Quyen.values())
            check(actualGrants.contains(role+":"+permission)==policy.isAllowed(role,permission),"Per-permission parity "+role+":"+permission);
        List<List<String>> missing=new ArrayList<>(grants);missing.removeFirst();
        check(!pairs(missing).equals(expected),"Parity detects deleted grant");
        List<List<String>> duplicate=new ArrayList<>(grants);duplicate.add(grants.getFirst());
        check(pairs(duplicate).size()!=duplicate.size(),"Duplicate seed detected");
        Set<String> widened=new HashSet<>(actualGrants);widened.add("NURSE:ACCOUNT_MANAGE");
        check(!widened.equals(expected),"Parity detects privilege expansion");

        Set<String> required=new TreeSet<>();
        Matcher names=Pattern.compile("\\(N'([A-Za-z0-9_]+)'\\)").matcher(section(sql,"REQUIRED TABLES"));
        int requiredRows=0;while(names.find()){requiredRows++;check(required.add(names.group(1)),"Duplicate table manifest");}
        Set<String> baselineTables=new TreeSet<>();
        try(var files=Files.list(bootstrap)) {
            for(Path file:files.filter(p->p.getFileName().toString().matches("V[1-8]__.*\\.sql")).toList()) {
                Matcher tables=Pattern.compile("(?i)CREATE\\s+TABLE\\s+dbo\\.([A-Za-z0-9_]+)").matcher(Files.readString(file));
                while(tables.find())baselineTables.add(tables.group(1));
            }
        }
        check(requiredRows==required.size()&&required.equals(baselineTables),"Named manifest equals V1-V8 tables");
        check(!code.contains("FROM sys.tables"),"No total-table-count gate");
        has(code,"OBJECT_ID(N'dbo.'+r.tableName,N'U')");has(code,"OBJECT_ID(N'dbo.DongTienHoaDon',N'V')");
        for(String column:List.of("donGiaBan","tenBenhNhanChot","soDienThoaiChot","tenHangChot"))has(code,column);
        for(String prerequisite:List.of("CK_TaiKhoan_VaiTro","CK_Thuoc_DonGiaBan","CK_ThanhToan_TrangThai","UX_LanThu_DangXuLy",
                "TR_HoaDon_ChotTen","TR_ChiTietHoaDon_ChotTen","TR_KeHoach_KhoaSauHoaDon"))has(code,prerequisite);
        has(code,"is_disabled=0 AND is_not_trusted=0");
        for(String table:List.of("VaiTro","Quyen","VaiTroQuyen","RbacEpoch")) {
            has(code,"CREATE TABLE dbo."+table+" (");has(code,"OBJECT_ID(N'dbo."+table+"') IS NOT NULL");
        }
        Matcher created=Pattern.compile("CREATE TABLE dbo\\.(\\w+)").matcher(code);int createdCount=0;while(created.find())createdCount++;
        check(createdCount==4,"Only four new tables");
        has(code,"roleCode varchar(32) COLLATE Latin1_General_100_BIN2 NOT NULL");
        for(String column:List.of("roleName nvarchar(100) NOT NULL","description nvarchar(500) NOT NULL","enabled bit NOT NULL",
                "isSystem bit NOT NULL","isProtected bit NOT NULL","revision rowversion NOT NULL"))has(code,column);
        has(code,"DATALENGTH(roleCode)=DATALENGTH(TRIM(roleCode))");has(code,"LIKE '[A-Z]'");has(code,"NOT LIKE '%[^A-Z0-9_]%'");
        has(code,"roleCode='SYSTEM_ADMIN' AND isSystem=1 AND isProtected=1 AND enabled=1");
        has(code,"roleCode<>'SYSTEM_ADMIN' AND isProtected=0");
        has(code,"PRIMARY KEY(roleCode,permissionCode)");has(code,"REFERENCES dbo.VaiTro(roleCode)");has(code,"REFERENCES dbo.Quyen(permissionCode)");
        check(!code.contains("CASCADE"),"No cascading policy deletion");
        has(code,"CHECK(singletonId=1)");has(code,"CHECK(epoch>=1)");has(code,"INSERT dbo.RbacEpoch(singletonId,epoch) VALUES(1,1)");
        check(Pattern.compile("(?i)\\bEXCEPT\\b").matcher(code).results().count()==6,"Bidirectional role/permission/grant postconditions");
        check(!Pattern.compile("(?im)^\\s*(ALTER|UPDATE|DELETE|INSERT(?: INTO)?)\\s+(?:TABLE\\s+)?dbo\\.TaiKhoan\\b").matcher(code).find(),"Account schema/data unchanged");
        check(!Pattern.compile("(?im)^\\s*(GRANT|DENY|REVOKE|CREATE (?:OR ALTER )?(?:PROCEDURE|TRIGGER))\\b").matcher(code).find(),"No access expansion/mutation endpoints");
        has(code,"DATALENGTH(DB_NAME()) <> 30");has(code,"LEFT(DB_NAME(),7) COLLATE Latin1_General_100_BIN2 <> N'N08Dev_'");
        has(code,"SUBSTRING(DB_NAME(),8,8) COLLATE Latin1_General_100_BIN2 LIKE N'%[^0-9a-f]%'");
        for(String db:List.of("N08Dev_55f617cc","N08Dev_00000000","N08Dev_ffffffff"))check(db.matches("N08Dev_[0-9a-f]{8}"),"Valid isolated DB");
        for(String db:List.of("N08Dev","n08dev_55f617cc","N08Dev_55F617CC","N08Dev_55f617cc ","N08Dev_1234567","N08Dev_123456789","N08Dev_55f617cg","Other_55f617cc"))
            check(!db.matches("N08Dev_[0-9a-f]{8}"),"Invalid DB rejected by contract");
        int guard=code.indexOf("IF @@TRANCOUNT <> 0"),beginTry=code.indexOf("BEGIN TRY"),begin=code.indexOf("BEGIN TRANSACTION;"),
                ddl=code.indexOf("CREATE TABLE dbo."),commit=code.indexOf("COMMIT TRANSACTION;"),catchAt=code.indexOf("BEGIN CATCH");
        check(guard<beginTry&&beginTry<begin&&begin<ddl&&ddl<commit&&commit<catchAt,"Standalone atomic migration ordering");
        has(code,"SET XACT_ABORT ON;");has(code,"@LockOwner='Transaction'");
        has(code,"IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;");has(code,"THROW;\nEND CATCH;");
        Matcher mutations=Pattern.compile("(?im)^\\s*(?:CREATE TABLE|INSERT dbo\\.)").matcher(code);
        while(mutations.find())check(mutations.start()>=begin&&mutations.start()<commit,"DDL/seed remains inside transaction");
        check(code.indexOf("THROW 51905")<ddl,"Existing/partial schema fails before DDL");
        String setup=Files.readString(root.resolve("src/main/java/com/quanlynhakhoa/hatang/phattrien/DevSetup.java"));
        check(!setup.contains("V9__"),"Not automatically bootstrapped");
        System.out.println("PASS: V9 static/domain contracts ("+checks+" checks;6 roles/26 permissions/63 exact grants;"+required.size()+" named prerequisite tables). NO SQL/JDBC EXECUTION; rollback enforcement NOT VERIFIED.");
    }
}
