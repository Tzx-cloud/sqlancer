package sqlancer.sqlite3.gen;

import sqlancer.BaseConfigurationGenerator;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mysql.gen.MySQLSetGenerator;
import sqlancer.sqlite3.SQLite3GlobalState;

import java.util.function.Function;


public class SQLite3SetGenerator extends BaseConfigurationGenerator {
    private static volatile SQLite3SetGenerator INSTANCE;

    private final StringBuilder sb = new StringBuilder();

    // currently, global options are only generated when a single thread is executed
    private boolean isSingleThreaded;

    public SQLite3SetGenerator(Randomly r, MainOptions options) {
        super(r, options);
    }

    @Override
    protected String getDatabaseType() {
        return "sqlite3";
    }

    public static SQLQueryAdapter set(SQLite3GlobalState globalState) {
        return new SQLite3SetGenerator(globalState.getRandomly(), globalState.getOptions()).get();
    }
    @Override
    protected String getActionName(Object action) {
        return ((SQLite3SetGenerator.Action) action).name();
    }

    @Override
    public  ConfigurationAction[] getAllActions() {
        return SQLite3SetGenerator.Action.values();
    }


    @Override
    public SQLQueryAdapter generateConfigForParameter(ConfigurationAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append("PRAGMA ");


        sb.append(action.getName());

        if(action.getScopes()[0]==Scope.GLOBAL) {
            sb.append(" = ");
            sb.append(action.generateValue(r));
        }else {
            if(action.generateValue(r)!="") {
                sb.append("(");
                sb.append(action.generateValue(r));
                sb.append(")");
            }
        }

        return new SQLQueryAdapter(sb.toString());
    }

    private SQLQueryAdapter get() {
        sb.append("SET ");
        SQLite3SetGenerator.Action a;
        if (isSingleThreaded) {
            a = Randomly.fromOptions(SQLite3SetGenerator.Action.values());
            Scope[] scopes = a.getScopes();
            Scope randomScope = Randomly.fromOptions(scopes);
            switch (randomScope) {
                case GLOBAL:
                    sb.append("GLOBAL");
                    break;
                case SESSION:
                    sb.append("SESSION");
                    break;
                default:
                    throw new AssertionError(randomScope);
            }

        } else {
            do {
                a = Randomly.fromOptions(SQLite3SetGenerator.Action.values());
            } while (!a.canBeUsedInScope(Scope.SESSION));
            sb.append("SESSION");
        }
        sb.append(" ");
        sb.append(a.getName());
        sb.append(" = ");
        sb.append(a.generateValue(r));
        return new SQLQueryAdapter(sb.toString());
    }

    @Override
    public SQLQueryAdapter generateDefaultConfigForParameter(ConfigurationAction action) {

        StringBuilder sb = new StringBuilder();
        sb.append("PRAGMA ");


        sb.append(action.getName());

        if(action.getScopes()[0]==Scope.GLOBAL) {
            sb.append(" = ");
            sb.append(action.getDefaultValue());
        }else {
            if(action.generateValue(r)!="") {
                sb.append("(");
                sb.append(action.getDefaultValue());
                sb.append(")");
            }
        }

        return new SQLQueryAdapter(sb.toString());

    }

    public static BaseConfigurationGenerator getInstance(Randomly r, MainOptions options) {
        if (INSTANCE == null) {
            synchronized (SQLite3SetGenerator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SQLite3SetGenerator(r, options);
                }
            }
        }
        return INSTANCE;
    }

    private enum Action implements ConfigurationAction{
        // SQLite PRAGMA 配置参数
// 查询分析限制
        SECURE_DELETE("secure_delete", (r) -> Randomly.fromOptions("OFF", "ON", "FAST"), "OFF", Scope.GLOBAL),
        ANALYSIS_LIMIT("analysis_limit", (r) -> r.getLongWithBoundaryBias(0, 1000000), "0", Scope.GLOBAL),

        // 应用程序ID
        APPLICATION_ID("application_id", (r) -> r.getLongWithBoundaryBias(0, 2147483647L), "0", Scope.GLOBAL),

        // 自动清理模式
        AUTO_VACUUM("auto_vacuum", (r) -> Randomly.fromOptions("NONE", "FULL", "INCREMENTAL"), "NONE", Scope.GLOBAL),

        // 自动索引
        AUTOMATIC_INDEX("automatic_index", (r) -> Randomly.fromOptions("OFF", "ON"), "ON", Scope.GLOBAL),

        // 忙等待超时（毫秒）
        BUSY_TIMEOUT("busy_timeout", (r) -> r.getLongWithBoundaryBias(0, 60000), "0", Scope.GLOBAL),

        // 缓存大小（负数为KB，正数为页数）
        CACHE_SIZE("cache_size", (r) -> r.getLongWithBoundaryBias(-65536, 65536), "-2000", Scope.GLOBAL),

        // 缓存溢出阈值
        CACHE_SPILL("cache_spill", (r) -> Randomly.fromOptions("OFF", "ON"), "ON", Scope.GLOBAL),

        // 单元格大小检查
        CELL_SIZE_CHECK("cell_size_check", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 检查点完全同步
        CHECKPOINT_FULLFSYNC("checkpoint_fullfsync", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 延迟外键检查
        DEFER_FOREIGN_KEYS("defer_foreign_keys", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 编码格式
        ENCODING("encoding", (r) -> Randomly.fromOptions("'UTF-8'", "'UTF-16'", "'UTF-16le'", "'UTF-16be'"), "'UTF-8'", Scope.GLOBAL),

        // 外键约束
        FOREIGN_KEYS("foreign_keys", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 完全同步
        FULLFSYNC("fullfsync", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 硬堆限制（字节）
        HARD_HEAP_LIMIT("hard_heap_limit", (r) -> r.getLongWithBoundaryBias(0, 1073741824L), "0", Scope.GLOBAL),

        // 完整性检查
        INTEGRITY_CHECK("integrity_check", (r) -> r.getLongWithBoundaryBias(0, 100L), "100", Scope.SESSION),

        // 忽略检查约束
        IGNORE_CHECK_CONSTRAINTS("ignore_check_constraints", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 增量清理页数
        INCREMENTAL_VACUUM("incremental_vacuum", (r) -> r.getLongWithBoundaryBias(0, 10000), "0", Scope.SESSION),

        // 日志模式
        JOURNAL_MODE("journal_mode", (r) -> Randomly.fromOptions("DELETE", "TRUNCATE", "PERSIST", "MEMORY", "WAL", "OFF"), "DELETE", Scope.GLOBAL),

        // 日志大小限制（字节）
        JOURNAL_SIZE_LIMIT("journal_size_limit", (r) -> r.getLongWithBoundaryBias(-1, 1073741824L), "-1", Scope.GLOBAL),

        // 传统ALTER TABLE行为
        LEGACY_ALTER_TABLE("legacy_alter_table", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 锁定模式
        LOCKING_MODE("locking_mode", (r) -> Randomly.fromOptions("NORMAL", "EXCLUSIVE"), "NORMAL", Scope.GLOBAL),

        // 最大页数
        MAX_PAGE_COUNT("max_page_count", (r) -> r.getLongWithBoundaryBias(1, 4294967294L), "1073741823", Scope.GLOBAL),

        // 内存映射大小（字节）
        MMAP_SIZE("mmap_size", (r) -> r.getLongWithBoundaryBias(0, 1073741824L), "0", Scope.GLOBAL),

        // 优化
        OPTIMIZE("optimize", (r) -> "", "", Scope.SESSION),

        // 页面大小（必须是2的幂，512-65536）
        PAGE_SIZE("page_size", (r) -> Randomly.fromOptions("512", "1024", "2048", "4096", "8192", "16384", "32768", "65536"), "4096", Scope.GLOBAL),

        // 解析器跟踪（调试用）
        PARSER_TRACE("parser_trace", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 只读查询
        QUERY_ONLY("query_only", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 读未提交
        READ_UNCOMMITTED("read_uncommitted", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 递归触发器
        RECURSIVE_TRIGGERS("recursive_triggers", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 反向无序选择
        REVERSE_UNORDERED_SELECTS("reverse_unordered_selects", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // 安全删除
        //SECURE_DELETE("secure_delete", (r) -> Randomly.fromOptions("OFF", "ON", "FAST"), "OFF", Scope.GLOBAL),

        // 软堆限制（字节）
        SOFT_HEAP_LIMIT("soft_heap_limit", (r) -> r.getLongWithBoundaryBias(0, 1073741824L), "0", Scope.GLOBAL),

        // 收缩内存
        SHRINK_MEMORY("shrink_memory", (r) -> "", "", Scope.SESSION),

        // 同步模式
        SYNCHRONOUS("synchronous", (r) -> Randomly.fromOptions("OFF", "NORMAL", "FULL", "EXTRA"), "FULL", Scope.GLOBAL),

        // 统计信息
        STATS("stats", (r) -> "", "", Scope.SESSION),

        // 临时存储
        TEMP_STORE("temp_store", (r) -> Randomly.fromOptions("DEFAULT", "FILE", "MEMORY"), "DEFAULT", Scope.GLOBAL),

        // 工作线程数
        THREADS("threads", (r) -> r.getLongWithBoundaryBias(0, 8), "0", Scope.GLOBAL),

        // 信任Schema
        TRUSTED_SCHEMA("trusted_schema", (r) -> Randomly.fromOptions("OFF", "ON"), "ON", Scope.GLOBAL),

        // VDBE添加操作跟踪（调试用）
        VDBE_ADDOPTRACE("vdbe_addoptrace", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // VDBE调试（调试用）
        VDBE_DEBUG("vdbe_debug", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // VDBE查询计划
        VDBE_EQP("vdbe_eqp", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // VDBE列表（调试用）
        VDBE_LISTING("vdbe_listing", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // VDBE跟踪（调试用）
        VDBE_TRACE("vdbe_trace", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        // WAL自动检查点
        WAL_AUTOCHECKPOINT("wal_autocheckpoint", (r) -> r.getLongWithBoundaryBias(0, 10000), "1000", Scope.GLOBAL),

        // 可写Schema
        WRITABLE_SCHEMA("writable_schema", (r) -> Randomly.fromOptions("OFF", "ON"), "OFF", Scope.GLOBAL),

        WAL_CHECKPOINT("wal_checkpoint", (r) -> Randomly.fromOptions("PASSIVE", "FULL", "RESTART","TRUNCATE"), "PASSIVE",Scope.SESSION);
        private final GenericAction delegate;
        private String defaultVaule = null;
        Action(String name, Function<Randomly, Object> prod,String defaultVaule, Scope... scopes) {
            this.delegate = new GenericAction(name, prod, scopes);
            this.defaultVaule = defaultVaule ;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object generateValue(Randomly r) {
            return delegate.generateValue(r);
        }

        @Override
        public Scope[] getScopes() {
            return delegate.getScopes();
        }

        @Override
        public boolean canBeUsedInScope(Scope scope) {
            return delegate.canBeUsedInScope(scope);
        }

        @Override
        public String getDefaultValue() {
            return defaultVaule;
        }
    }
}
