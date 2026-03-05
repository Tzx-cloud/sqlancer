package sqlancer.mariadb.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import sqlancer.BaseConfigurationGenerator;
import sqlancer.MainOptions;
import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mariadb.MariaDBBugs;
import sqlancer.mariadb.MariaDBProvider.MariaDBGlobalState;
import sqlancer.mysql.gen.MySQLSetGenerator;

public class MariaDBSetGenerator extends BaseConfigurationGenerator {
    private static volatile MariaDBSetGenerator INSTANCE;
    private final StringBuilder sb = new StringBuilder();

    // currently, global options are only generated when a single thread is executed
    private boolean isSingleThreaded;

    public MariaDBSetGenerator(Randomly r, MainOptions options) {
        super(r,options);
    }

    public static SQLQueryAdapter set(Randomly r, MainOptions options) {
        return new MariaDBSetGenerator(r, options).get();
    }

    @Override
    protected String getDatabaseType() {
        return "mariadb";
    }

    @Override
    protected String getActionName(Object action) {
        return ((Action) action).name();
    }

    @Override
    public  ConfigurationAction[] getAllActions() {
        return Action.values();
    }


    @Override
    public SQLQueryAdapter generateConfigForParameter(ConfigurationAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append("SET ");

        // 选择作用域
        Scope[] scopes = action.getScopes();
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

        sb.append(" ");
        sb.append(action.getName());
        sb.append(" = ");
        sb.append(action.generateValue(r));

        return new SQLQueryAdapter(sb.toString());
    }

    private SQLQueryAdapter get() {
        sb.append("SET ");
        Action a;
        if (isSingleThreaded) {
            a = Randomly.fromOptions(Action.values());
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
                a = Randomly.fromOptions(Action.values());
            } while (!a.canBeUsedInScope(Scope.SESSION));
            sb.append("SESSION");
        }
        sb.append(" ");
        sb.append(a.getName());
        sb.append(" = ");
        sb.append(a.generateValue(r));
        return new SQLQueryAdapter(sb.toString(), ExpectedErrors
                .from("At least one of the 'in_to_exists' or 'materialization' optimizer_switch flags must be 'on'"));
    }

    @Override
    public SQLQueryAdapter generateDefaultConfigForParameter(ConfigurationAction action) {
        if (action.getName()=="optimizer_switch") {
            return resetOptimizer();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SET ");

        // 选择作用域
        Scope[] scopes = action.getScopes();

        if(scopes.length==2||scopes[0]==Scope.GLOBAL )sb.append("GLOBAL");
        else sb.append("SESSION");

        sb.append(" ");
        sb.append(action.getName());
        sb.append(" = DEFAULT");

        return new SQLQueryAdapter(sb.toString());
    }

    public static BaseConfigurationGenerator getInstance(Randomly r, MainOptions options) {
        if (INSTANCE == null) {
            synchronized (MariaDBSetGenerator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MariaDBSetGenerator(r, options);
                }
            }
        }
        return INSTANCE;
    }

    public static SQLQueryAdapter resetOptimizer() {
        return new SQLQueryAdapter("SET optimizer_switch='default'");
    }


    private enum Action implements ConfigurationAction {

        ALTER_ALGORITHM("alter_algorithm", (r) -> Randomly.fromOptions( "COPY", "INPLACE", "INSTANT", "NOCOPY"), Scope.GLOBAL, Scope.SESSION), //
        ANALYZE_SAMPLE_PERCENTAGE("analyze_sample_percentage", (r) -> r.getLongWithBoundaryBias(0, 100), Scope.GLOBAL, Scope.SESSION), //
        AUTOCOMMIT("autocommit", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        AUTOMATIC_SP_PRIVILEGES("automatic_sp_privileges", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        AUTO_INCREMENT_INCREMENT("auto_increment_increment", (r) -> r.getLongWithBoundaryBias(1, 65535), Scope.GLOBAL, Scope.SESSION), //
        AUTO_INCREMENT_OFFSET("auto_increment_offset", (r) -> r.getLongWithBoundaryBias(1, 65535), Scope.GLOBAL, Scope.SESSION), //
        //BACK_LOG("back_log", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL), //
        //BASEDIR("basedir", (r) -> r.getString(), Scope.GLOBAL), //
        BIG_TABLES("big_tables", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        //BIND_ADDRESS("bind_address", (r) -> r.getString(), Scope.GLOBAL), //
        BINLOG_ANNOTATE_ROW_EVENTS("binlog_annotate_row_events", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_CACHE_SIZE("binlog_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL),//
        BINLOG_COMMIT_WAIT_COUNT("binlog_commit_wait_count", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        BINLOG_COMMIT_WAIT_USEC("binlog_commit_wait_usec", (r) -> r.getLongWithBoundaryBias(0, 1000000), Scope.GLOBAL), //
        BINLOG_DIRECT_NON_TRANSACTIONAL_UPDATES("binlog_direct_non_transactional_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        BINLOG_EXPIRE_LOGS_SECONDS("binlog_expire_logs_seconds", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        BINLOG_FILE_CACHE_SIZE("binlog_file_cache_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL),//
        BINLOG_FORMAT("binlog_format", (r) -> Randomly.fromOptions("ROW", "STATEMENT", "MIXED"), Scope.GLOBAL, Scope.SESSION),//
        BINLOG_ROW_IMAGE("binlog_row_image", (r) -> Randomly.fromOptions("FULL", "MINIMAL", "NOBLOB"), Scope.GLOBAL, Scope.SESSION),//
        BINLOG_ROW_METADATA("binlog_row_metadata", (r) -> Randomly.fromOptions("MINIMAL", "FULL"), Scope.GLOBAL), //
        BINLOG_STMT_CACHE_SIZE("binlog_stmt_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        BULK_INSERT_BUFFER_SIZE("bulk_insert_buffer_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SETS_DIR("character_sets_dir", (r) -> r.getString(), Scope.GLOBAL), //
//        CHARACTER_SET_CLIENT("character_set_client", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_CONNECTION("character_set_connection", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION),//
//        CHARACTER_SET_DATABASE("character_set_database", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_FILESYSTEM("character_set_filesystem", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_RESULTS("character_set_results", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION),//
//        CHARACTER_SET_SERVER("character_set_server", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        CHARACTER_SET_SYSTEM("character_set_system", (r) -> r.getString(), Scope.GLOBAL), //
        CHECK_CONSTRAINT_CHECKS("check_constraint_checks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
//        COLLATION_CONNECTION("collation_connection", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION),//
//        COLLATION_DATABASE("collation_database", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        COLLATION_SERVER("collation_server", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        COLUMN_COMPRESSION_THRESHOLD("column_compression_threshold", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        COLUMN_COMPRESSION_ZLIB_LEVEL("column_compression_zlib_level", (r) -> r.getLongWithBoundaryBias(0, 9), Scope.GLOBAL, Scope.SESSION), //
        COLUMN_COMPRESSION_ZLIB_STRATEGY("column_compression_zlib_strategy", (r) -> Randomly.fromOptions( "FILTERED", "HUFFMAN_ONLY", "RLE", "FIXED"), Scope.GLOBAL, Scope.SESSION), //
        COLUMN_COMPRESSION_ZLIB_WRAP("column_compression_zlib_wrap", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        COMPLETION_TYPE("completion_type", (r) -> Randomly.fromOptions("'NO_CHAIN'", "'CHAIN'", "'RELEASE'"), Scope.GLOBAL, Scope.SESSION), //
        CONCURRENT_INSERT("concurrent_insert", (r) -> Randomly.fromOptions("NEVER", "AUTO", "ALWAYS"), Scope.GLOBAL), //
        CONNECT_TIMEOUT("connect_timeout", (r) -> r.getLongWithBoundaryBias(2, 31536000), Scope.GLOBAL), //
        // CORE_FILE("core_file", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        DATADIR("datadir", (r) -> r.getString(), Scope.GLOBAL), //
//        DATE_FORMAT("date_format", (r) -> r.getString(), Scope.GLOBAL), //
        DEADLOCK_SEARCH_DEPTH_LONG("deadlock_search_depth_long", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        DEADLOCK_SEARCH_DEPTH_SHORT("deadlock_search_depth_short", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        DEADLOCK_TIMEOUT_LONG("deadlock_timeout_long", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        DEADLOCK_TIMEOUT_SHORT("deadlock_timeout_short", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        //DEFAULT_MASTER_CONNECTION("default_master_connection", (r) -> r.getString(), Scope.SESSION), //
        DEFAULT_PASSWORD_LIFETIME("default_password_lifetime", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL), //
        //DEFAULT_REGEX_FLAGS("default_regex_flags", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        //DEFAULT_STORAGE_ENGINE("default_storage_engine", (r) -> Randomly.fromOptions("InnoDB", "MyISAM", "Aria"), Scope.GLOBAL, Scope.SESSION), //
        //DEFAULT_TMP_STORAGE_ENGINE("default_tmp_storage_engine", (r) -> Randomly.fromOptions("MyISAM", "Aria", "InnoDB"), Scope.GLOBAL, Scope.SESSION), //
        DEFAULT_WEEK_FORMAT("default_week_format", (r) -> r.getLongWithBoundaryBias(0, 7), Scope.GLOBAL, Scope.SESSION), //
        DELAYED_INSERT_LIMIT("delayed_insert_limit", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        DELAYED_INSERT_TIMEOUT("delayed_insert_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL), //
        DELAYED_QUEUE_SIZE("delayed_queue_size", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        DELAY_KEY_WRITE("delay_key_write", (r) -> Randomly.fromOptions("OFF", "ON", "ALL"), Scope.GLOBAL),//
        DISCONNECT_ON_EXPIRED_PASSWORD("disconnect_on_expired_password", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        DIV_PRECISION_INCREMENT("div_precision_increment", (r) -> r.getLongWithBoundaryBias(0, 30), Scope.GLOBAL, Scope.SESSION), //
        //ENCRYPT_BINLOG("encrypt_binlog", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        ENCRYPT_TMP_DISK_TABLES("encrypt_tmp_disk_tables", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //ENCRYPT_TMP_FILES("encrypt_tmp_files", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //ENFORCE_STORAGE_ENGINE("enforce_storage_engine", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        EQ_RANGE_INDEX_DIVE_LIMIT("eq_range_index_dive_limit", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION),//
        //ERROR_COUNT("error_count", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.SESSION), //
        EVENT_SCHEDULER("event_scheduler", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        EXPENSIVE_SUBQUERY_LIMIT("expensive_subquery_limit", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        EXPIRE_LOGS_DAYS("expire_logs_days", (r) -> r.getLongWithBoundaryBias(0, 99), Scope.GLOBAL), //
        EXPLICIT_DEFAULTS_FOR_TIMESTAMP("explicit_defaults_for_timestamp", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        //EXTERNAL_USER("external_user", (r) -> r.getString(), Scope.SESSION), //
        EXTRA_MAX_CONNECTIONS("extra_max_connections", (r) -> r.getLongWithBoundaryBias(1, 100000), Scope.GLOBAL),//
        //EXTRA_PORT("extra_port", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL), //
        FLUSH("flush", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        FLUSH_TIME("flush_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        FOREIGN_KEY_CHECKS("foreign_key_checks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        //FT_BOOLEAN_SYNTAX("ft_boolean_syntax", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        //FT_MAX_WORD_LEN("ft_max_word_len", (r) -> r.getLongWithBoundaryBias(10, 4294967295L), Scope.GLOBAL), //
        //FT_MIN_WORD_LEN("ft_min_word_len", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        //FT_QUERY_EXPANSION_LIMIT("ft_query_expansion_limit", (r) -> r.getLongWithBoundaryBias(0, 1000), Scope.GLOBAL), //
        //FT_STOPWORD_FILE("ft_stopword_file", (r) -> r.getString(), Scope.GLOBAL), //
        GENERAL_LOG("general_log", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        // GENERAL_LOG_FILE("general_log_file", (r) -> r.getString(), Scope.GLOBAL), //
        GROUP_CONCAT_MAX_LEN("group_concat_max_len", (r) -> r.getLongWithBoundaryBias(4, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        //GTID_BINLOG_POS("gtid_binlog_pos", (r) -> r.getString(), Scope.GLOBAL), //
        //GTID_BINLOG_STATE("gtid_binlog_state", (r) -> r.getString(), Scope.GLOBAL),//
        GTID_CLEANUP_BATCH_SIZE("gtid_cleanup_batch_size", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        //GTID_CURRENT_POS("gtid_current_pos", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        GTID_DOMAIN_ID("gtid_domain_id", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        GTID_IGNORE_DUPLICATES("gtid_ignore_duplicates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //GTID_POS_AUTO_ENGINES("gtid_pos_auto_engines", (r) -> r.getString(), Scope.GLOBAL), //
        GTID_SEQ_NO("gtid_seq_no", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        //GTID_SLAVE_POS("gtid_slave_pos", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        GTID_STRICT_MODE("gtid_strict_mode", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //HAVE_COMPRESS("have_compress", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL),//
        //HAVE_CRYPT("have_crypt", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL), //
        //HAVE_DYNAMIC_LOADING("have_dynamic_loading", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL), //
//        HAVE_GEOMETRY("have_geometry", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL), //
//        HAVE_OPENSSL("have_openssl", (r) -> Randomly.fromOptions("YES", "NO", "DISABLED"), Scope.GLOBAL), //
//        HAVE_PROFILING("have_profiling", (r) -> Randomly.fromOptions("YES", "NO", "DISABLED"), Scope.GLOBAL), //
//        HAVE_QUERY_CACHE("have_query_cache", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL), //
//        HAVE_RTREE_KEYS("have_rtree_keys", (r) -> Randomly.fromOptions("YES", "NO", "DISABLED"), Scope.GLOBAL), //
//        HAVE_SSL("have_ssl", (r) -> Randomly.fromOptions("YES", "NO", "DISABLED"), Scope.GLOBAL), //
//        HAVE_SYMLINK("have_symlink", (r) -> Randomly.fromOptions("YES", "NO", "DISABLED"), Scope.GLOBAL), //
        HISTOGRAM_SIZE("histogram_size", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL, Scope.SESSION), //
        HISTOGRAM_TYPE("histogram_type", (r) -> Randomly.fromOptions("SINGLE_PREC_HB", "DOUBLE_PREC_HB"), Scope.GLOBAL, Scope.SESSION), //
        //HOSTNAME("hostname", (r) -> r.getString(), Scope.GLOBAL), //
        HOST_CACHE_SIZE("host_cache_size", (r) -> r.getLongWithBoundaryBias(0, 65536), Scope.GLOBAL), //
        //IDENTITY("identity", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        IDLE_READONLY_TRANSACTION_TIMEOUT("idle_readonly_transaction_timeout", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        IDLE_TRANSACTION_TIMEOUT("idle_transaction_timeout", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        IDLE_WRITE_TRANSACTION_TIMEOUT("idle_write_transaction_timeout", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        //IGNORE_BUILTIN_INNODB("ignore_builtin_innodb", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        IGNORE_DB_DIRS("ignore_db_dirs", (r) -> r.getString(), Scope.GLOBAL), //
//        INIT_CONNECT("init_connect", (r) -> r.getString(), Scope.GLOBAL), //
//        INIT_FILE("init_file", (r) -> r.getString(), Scope.GLOBAL), //
//        INIT_SLAVE("init_slave", (r) -> r.getString(), Scope.GLOBAL),//
        INNODB_ADAPTIVE_FLUSHING("innodb_adaptive_flushing", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_ADAPTIVE_FLUSHING_LWM("innodb_adaptive_flushing_lwm", (r) -> r.getLongWithBoundaryBias(0, 70), Scope.GLOBAL), //
        //INNODB_AUTOINC_LOCK_MODE("innodb_autoinc_lock_mode", (r) -> r.getLongWithBoundaryBias(0, 2), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_DUMP_AT_SHUTDOWN("innodb_buffer_pool_dump_at_shutdown", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_DUMP_PCT("innodb_buffer_pool_dump_pct", (r) -> r.getLongWithBoundaryBias(1, 100), Scope.GLOBAL), //
        //INNODB_BUFFER_POOL_LOAD_AT_STARTUP("innodb_buffer_pool_load_at_startup", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_BUFFER_POOL_SIZE("innodb_buffer_pool_size", (r) -> r.getLongWithBoundaryBias(5242880, Long.MAX_VALUE), Scope.GLOBAL), //
        // INNODB_CHANGE_BUFFERING("innodb_change_buffering", (r) -> Randomly.fromOptions("none", "inserts", "deletes", "changes", "purges", "all"), Scope.GLOBAL), //
        //INNODB_CHANGE_BUFFER_MAX_SIZE("innodb_change_buffer_max_size", (r) -> r.getLongWithBoundaryBias(0, 50), Scope.GLOBAL), //
        //INNODB_CHECKSUM_ALGORITHM("innodb_checksum_algorithm", (r) -> Randomly.fromOptions("crc32", "strict_crc32", "innodb", "strict_innodb", "none"), Scope.GLOBAL), //
        INNODB_CMP_PER_INDEX_ENABLED("innodb_cmp_per_index_enabled", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_COMPRESSION_ALGORITHM("innodb_compression_algorithm", (r) -> Randomly.fromOptions("none", "zlib", "lz4", "lzo", "lzma", "bzip2", "snappy"), Scope.GLOBAL), //
        INNODB_DEADLOCK_DETECT("innodb_deadlock_detect", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        // INNODB_DEADLOCK_REPORT("innodb_deadlock_report", (r) -> Randomly.fromOptions("OFF", "ON", "BASIC", "FULL"), Scope.GLOBAL), //
        INNODB_DEFAULT_ROW_FORMAT("innodb_default_row_format", (r) -> Randomly.fromOptions("redundant", "compact", "dynamic"), Scope.GLOBAL), //
        INNODB_DISABLE_SORT_FILE_CACHE("innodb_disable_sort_file_cache", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        //INNODB_ENCRYPT_LOG("innodb_encrypt_log", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        //INNODB_ENCRYPT_TABLES("innodb_encrypt_tables", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_ENCRYPT_TEMPORARY_TABLES("innodb_encrypt_temporary_tables", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        INNODB_FAST_SHUTDOWN("innodb_fast_shutdown", (r) -> r.getLongWithBoundaryBias(0, 2), Scope.GLOBAL), //
        //INNODB_FATAL_SEMAPHORE_WAIT_THRESHOLD("innodb_fatal_semaphore_wait_threshold", (r) -> r.getLongWithBoundaryBias(1, 3600), Scope.GLOBAL), //
        INNODB_FILE_PER_TABLE("innodb_file_per_table", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_FLUSHING_AVG_LOOPS("innodb_flushing_avg_loops", (r) -> r.getLongWithBoundaryBias(1, 1000), Scope.GLOBAL), //
        INNODB_FLUSH_LOG_AT_TIMEOUT("innodb_flush_log_at_timeout", (r) -> r.getLongWithBoundaryBias(0, 2700), Scope.GLOBAL), //
        INNODB_FLUSH_LOG_AT_TRX_COMMIT("innodb_flush_log_at_trx_commit", (r) -> r.getLongWithBoundaryBias(0, 2), Scope.GLOBAL), //
        // INNODB_FLUSH_METHOD("innodb_flush_method", (r) -> Randomly.fromOptions("fsync", "O_DSYNC", "littlesync", "nosync", "O_DIRECT", "O_DIRECT_NO_FSYNC"), Scope.GLOBAL), //
        INNODB_FLUSH_NEIGHBORS("innodb_flush_neighbors", (r) -> Randomly.fromOptions("0", "1", "2"), Scope.GLOBAL), //
        INNODB_FLUSH_SYNC("innodb_flush_sync", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_FORCE_PRIMARY_KEY("innodb_force_primary_key", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_FORCE_RECOVERY("innodb_force_recovery", (r) -> r.getLongWithBoundaryBias(0, 6), Scope.GLOBAL), //
        INNODB_IMMEDIATE_SCRUB_DATA_UNCOMPRESSED("innodb_immediate_scrub_data_uncompressed", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_INSTANT_ALTER_COLUMN_ALLOWED("innodb_instant_alter_column_allowed", (r) -> Randomly.fromOptions( "never", "add_last","add_drop_reorder"), Scope.GLOBAL), //
        INNODB_IO_CAPACITY("innodb_io_capacity", (r) -> r.getLongWithBoundaryBias(100, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_LOCK_WAIT_TIMEOUT("innodb_lock_wait_timeout", (r) -> r.getLongWithBoundaryBias(1, 1073741824), Scope.GLOBAL, Scope.SESSION), //
        //INNODB_LOG_BUFFER_SIZE("innodb_log_buffer_size", (r) -> r.getLongWithBoundaryBias(1048576, 4294967295L), Scope.GLOBAL), //
        INNODB_LOG_FILE_SIZE("innodb_log_file_size", (r) -> r.getLongWithBoundaryBias(1048576, 549755813888L), Scope.GLOBAL), //
        //INNODB_LOG_GROUP_HOME_DIR("innodb_log_group_home_dir", (r) -> r.getString(), Scope.GLOBAL), //
        //INNODB_LOG_WRITE_AHEAD_SIZE("innodb_log_write_ahead_size", (r) -> r.getLongWithBoundaryBias(512, 16384), Scope.GLOBAL), //
        INNODB_LRU_SCAN_DEPTH("innodb_lru_scan_depth", (r) -> r.getLongWithBoundaryBias(100, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_MAX_DIRTY_PAGES_PCT_LWM("innodb_max_dirty_pages_pct_lwm", (r) -> r.getLongWithBoundaryBias(0, 99), Scope.GLOBAL),//
        INNODB_MAX_PURGE_LAG("innodb_max_purge_lag", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        INNODB_MAX_PURGE_LAG_DELAY("innodb_max_purge_lag_delay", (r) -> r.getLongWithBoundaryBias(0, 10000000), Scope.GLOBAL), //
        INNODB_MAX_UNDO_LOG_SIZE("innodb_max_undo_log_size", (r) -> r.getLongWithBoundaryBias(10485760, Long.MAX_VALUE), Scope.GLOBAL), //
        //INNODB_NUMA_INTERLEAVE("innodb_numa_interleave", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_OPTIMIZE_FULLTEXT_ONLY("innodb_optimize_fulltext_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_PAGE_SIZE("innodb_page_size", (r) -> r.getLongWithBoundaryBias(4096, 65536), Scope.GLOBAL), //
        INNODB_PRINT_ALL_DEADLOCKS("innodb_print_all_deadlocks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_PURGE_BATCH_SIZE("innodb_purge_batch_size", (r) -> r.getLongWithBoundaryBias(1, 5000), Scope.GLOBAL), //
        INNODB_RANDOM_READ_AHEAD("innodb_random_read_ahead", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_READ_AHEAD_THRESHOLD("innodb_read_ahead_threshold", (r) -> r.getLongWithBoundaryBias(0, 64), Scope.GLOBAL), //
        INNODB_READ_ONLY_COMPRESSED("innodb_read_only_compressed", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_SPIN_WAIT_DELAY("innodb_spin_wait_delay", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        INNODB_STATS_AUTO_RECALC("innodb_stats_auto_recalc", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_STATS_INCLUDE_DELETE_MARKED("innodb_stats_include_delete_marked", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_STATS_METHOD("innodb_stats_method", (r) -> Randomly.fromOptions("nulls_equal", "nulls_unequal", "nulls_ignored"), Scope.GLOBAL), //
        INNODB_STATS_MODIFIED_COUNTER("innodb_stats_modified_counter", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        INNODB_STATS_PERSISTENT("innodb_stats_persistent", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INNODB_STATS_PERSISTENT_SAMPLE_PAGES("innodb_stats_persistent_sample_pages", (r) -> r.getLongWithBoundaryBias(0, 1000), Scope.GLOBAL), //
        INNODB_STATS_TRANSIENT_SAMPLE_PAGES("innodb_stats_transient_sample_pages", (r) -> r.getLongWithBoundaryBias(1, 1000), Scope.GLOBAL), //
        INNODB_UNDO_LOG_TRUNCATE("innodb_undo_log_truncate", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_UNDO_TABLESPACES("innodb_undo_tablespaces", (r) -> r.getLongWithBoundaryBias(0, 127), Scope.GLOBAL), //
        //INNODB_USE_ATOMIC_WRITES("innodb_use_atomic_writes", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //INNODB_USE_NATIVE_AIO("innodb_use_native_aio", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        INSERT_ID("insert_id", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        INTERACTIVE_TIMEOUT("interactive_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        IN_PREDICATE_CONVERSION_THRESHOLD("in_predicate_conversion_threshold", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        // IN_TRANSACTION("in_transaction", (r) -> r.getLongWithBoundaryBias(0, 1), Scope.SESSION), //
        JOIN_BUFFER_SIZE("join_buffer_size", (r) -> r.getLongWithBoundaryBias(128, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        JOIN_BUFFER_SPACE_LIMIT("join_buffer_space_limit", (r) -> r.getLongWithBoundaryBias(2048, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        JOIN_CACHE_LEVEL("join_cache_level", (r) -> r.getLongWithBoundaryBias(0, 8), Scope.GLOBAL, Scope.SESSION), //
        KEEP_FILES_ON_CREATE("keep_files_on_create", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        KEY_BUFFER_SIZE("key_buffer_size", (r) -> r.getLongWithBoundaryBias(8, Long.MAX_VALUE), Scope.GLOBAL), //
        KEY_CACHE_AGE_THRESHOLD("key_cache_age_threshold", (r) -> r.getLongWithBoundaryBias(100, Long.MAX_VALUE), Scope.GLOBAL), //
        KEY_CACHE_BLOCK_SIZE("key_cache_block_size", (r) -> r.getLongWithBoundaryBias(512, 16384), Scope.GLOBAL), //
        KEY_CACHE_DIVISION_LIMIT("key_cache_division_limit", (r) -> r.getLongWithBoundaryBias(1, 100), Scope.GLOBAL), //
        KEY_CACHE_FILE_HASH_SIZE("key_cache_file_hash_size", (r) -> r.getLongWithBoundaryBias(4, 16384), Scope.GLOBAL), //
        KEY_CACHE_SEGMENTS("key_cache_segments", (r) -> r.getLongWithBoundaryBias(0, 64), Scope.GLOBAL), //
        //LARGE_FILES_SUPPORT("large_files_support", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL), //
        //LARGE_PAGES("large_pages", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //LARGE_PAGE_SIZE("large_page_size", (r) -> r.getLongWithBoundaryBias(0, 65536), Scope.GLOBAL), //
//        LAST_GTID("last_gtid", (r) -> r.getString(), Scope.SESSION), //
//        LAST_INSERT_ID("last_insert_id", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
//        LC_MESSAGES("lc_messages", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        LC_MESSAGES_DIR("lc_messages_dir", (r) -> r.getString(), Scope.GLOBAL), //
//        LC_TIME_NAMES("lc_time_names", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        LICENSE("license", (r) -> r.getString(), Scope.GLOBAL), //
        LOCAL_INFILE("local_infile", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        //LOCKED_IN_MEMORY("locked_in_memory", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOCK_WAIT_TIMEOUT("lock_wait_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION),//
        //LOG_BIN("log_bin", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //LOG_BIN_BASENAME("log_bin_basename", (r) -> r.getString(), Scope.GLOBAL), //
        LOG_BIN_COMPRESS("log_bin_compress", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_BIN_COMPRESS_MIN_LEN("log_bin_compress_min_len", (r) -> r.getLongWithBoundaryBias(0, 1024), Scope.GLOBAL), //
        //LOG_BIN_INDEX("log_bin_index", (r) -> r.getString(), Scope.GLOBAL), //
        LOG_BIN_TRUST_FUNCTION_CREATORS("log_bin_trust_function_creators", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        LOG_DISABLED_STATEMENTS("log_disabled_statements", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
//        LOG_ERROR("log_error", (r) -> r.getString(), Scope.GLOBAL), //
        //LOG_OUTPUT("log_output", (r) -> Randomly.fromOptions("TABLE", "FILE", "NONE"), Scope.GLOBAL), //
        LOG_QUERIES_NOT_USING_INDEXES("log_queries_not_using_indexes", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //LOG_SLAVE_UPDATES("log_slave_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        LOG_SLOW_ADMIN_STATEMENTS("log_slow_admin_statements", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //LOG_SLOW_DISABLED_STATEMENTS("log_slow_disabled_statements", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION),//
        //LOG_SLOW_FILTER("log_slow_filter", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        LOG_SLOW_MAX_WARNINGS("log_slow_max_warnings", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        LOG_SLOW_RATE_LIMIT("log_slow_rate_limit", (r) -> r.getLongWithBoundaryBias(1, 1000000), Scope.GLOBAL, Scope.SESSION), //
        LOG_SLOW_SLAVE_STATEMENTS("log_slow_slave_statements", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //LOG_SLOW_VERBOSITY("log_slow_verbosity", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        //LOG_TC_SIZE("log_tc_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL), //
        LOG_WARNINGS("log_warnings", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        LONG_QUERY_TIME("long_query_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        //LOWER_CASE_FILE_SYSTEM("lower_case_file_system", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //LOWER_CASE_TABLE_NAMES("lower_case_table_names", (r) -> r.getLongWithBoundaryBias(0, 2), Scope.GLOBAL), //
        LOW_PRIORITY_UPDATES("low_priority_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        MASTER_VERIFY_CHECKSUM("master_verify_checksum", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        MAX_ALLOWED_PACKET("max_allowed_packet", (r) -> r.getLongWithBoundaryBias(1024, 1073741824), Scope.GLOBAL), //
        MAX_BINLOG_CACHE_SIZE("max_binlog_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        MAX_BINLOG_SIZE("max_binlog_size", (r) -> r.getLongWithBoundaryBias(4096, 1073741824), Scope.GLOBAL), //
        MAX_BINLOG_STMT_CACHE_SIZE("max_binlog_stmt_cache_size", (r) -> r.getLongWithBoundaryBias(4096, Long.MAX_VALUE), Scope.GLOBAL), //
        MAX_CONNECT_ERRORS("max_connect_errors", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        MAX_DELAYED_THREADS("max_delayed_threads", (r) -> r.getLongWithBoundaryBias(0, 16384), Scope.GLOBAL), //
        //MAX_DIGEST_LENGTH("max_digest_length", (r) -> r.getLongWithBoundaryBias(0, 1048576), Scope.GLOBAL), //
        MAX_ERROR_COUNT("max_error_count", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL, Scope.SESSION), //
        MAX_HEAP_TABLE_SIZE("max_heap_table_size", (r) -> r.getLongWithBoundaryBias(16384, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_INSERT_DELAYED_THREADS("max_insert_delayed_threads", (r) -> r.getLongWithBoundaryBias(0, 16384), Scope.GLOBAL), //
        MAX_JOIN_SIZE("max_join_size", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_LENGTH_FOR_SORT_DATA("max_length_for_sort_data", (r) -> r.getLongWithBoundaryBias(4, 8388608), Scope.GLOBAL, Scope.SESSION), //
        MAX_PASSWORD_ERRORS("max_password_errors", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        MAX_PREPARED_STMT_COUNT("max_prepared_stmt_count", (r) -> r.getLongWithBoundaryBias(0, 1048576), Scope.GLOBAL), //
        MAX_RECURSIVE_ITERATIONS("max_recursive_iterations", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        MAX_RELAY_LOG_SIZE("max_relay_log_size", (r) -> r.getLongWithBoundaryBias(0, 1073741824), Scope.GLOBAL), //
        MAX_ROWID_FILTER_SIZE("max_rowid_filter_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_SEEKS_FOR_KEY("max_seeks_for_key", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_SESSION_MEM_USED("max_session_mem_used", (r) -> r.getLongWithBoundaryBias(8192, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MAX_SORT_LENGTH("max_sort_length", (r) -> r.getLongWithBoundaryBias(4, 8388608), Scope.GLOBAL, Scope.SESSION), //
        MAX_SP_RECURSION_DEPTH("max_sp_recursion_depth", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL, Scope.SESSION), //
        MAX_STATEMENT_TIME("max_statement_time", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        //MAX_USER_CONNECTIONS("max_user_connections", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        MAX_WRITE_LOCK_COUNT("max_write_lock_count", (r) -> r.getLongWithBoundaryBias(1, Long.MAX_VALUE), Scope.GLOBAL), //
        //METADATA_LOCKS_CACHE_SIZE("metadata_locks_cache_size", (r) -> r.getLongWithBoundaryBias(1, 1048576), Scope.GLOBAL), //
        //METADATA_LOCKS_HASH_INSTANCES("metadata_locks_hash_instances", (r) -> r.getLongWithBoundaryBias(1, 1024), Scope.GLOBAL), //
        MIN_EXAMINED_ROW_LIMIT("min_examined_row_limit", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        MRR_BUFFER_SIZE("mrr_buffer_size", (r) -> r.getLongWithBoundaryBias(8192, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        MYSQL56_TEMPORAL_FORMAT("mysql56_temporal_format", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        NET_BUFFER_LENGTH("net_buffer_length", (r) -> r.getLongWithBoundaryBias(1024, 1048576), Scope.GLOBAL), //
        NET_READ_TIMEOUT("net_read_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        NET_RETRY_COUNT("net_retry_count", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        NET_WRITE_TIMEOUT("net_write_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION),//
        NOTE_VERBOSITY("note_verbosity",
                (r) -> Randomly.fromOptions(0, 1, 2, 3, 4),
                Scope.GLOBAL), //, //
        //OFFLINE_MODE("offline_mode", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        OLD("old", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        OLD_MODE("old_mode", (r) -> Randomly.fromOptions("COMPAT_5_1_CHECKSUM", "IGNORE_INDEX_ONLY_FOR_JOIN", "ZERO_DATE_TIME_CAST"), Scope.GLOBAL, Scope.SESSION), //
        OLD_PASSWORDS("old_passwords", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        //OPEN_FILES_LIMIT("open_files_limit", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        OPTIMIZER_ADJUST_SECONDARY_KEY_COSTS("optimizer_adjust_secondary_key_costs",
                (r) -> Randomly.fromOptions(0, 1),
                Scope.GLOBAL), //

        OPTIMIZER_JOIN_LIMIT_PREF_RATIO("optimizer_join_limit_pref_ratio", (r) -> r.getLongWithBoundaryBias(0, 1000), Scope.GLOBAL, Scope.SESSION),//
        OPTIMIZER_MAX_SEL_ARGS("optimizer_max_sel_args", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_MAX_SEL_ARG_WEIGHT("optimizer_max_sel_arg_weight", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_PRUNE_LEVEL("optimizer_prune_level", (r) -> r.getLongWithBoundaryBias(0, 1), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_SEARCH_DEPTH("optimizer_search_depth", (r) -> r.getLongWithBoundaryBias(0, 62), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_SELECTIVITY_SAMPLING_LIMIT("optimizer_selectivity_sampling_limit", (r) -> r.getLongWithBoundaryBias(10, 100000), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_SWITCH("optimizer_switch", (r) -> getOptimizerSwitchConfiguration(r), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_TRACE("optimizer_trace",
                (r) -> "'enabled=" + Randomly.fromOptions("on'", "off'"),
                Scope.SESSION), //

        OPTIMIZER_TRACE_MAX_MEM_SIZE("optimizer_trace_max_mem_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        OPTIMIZER_USE_CONDITION_SELECTIVITY("optimizer_use_condition_selectivity", (r) -> r.getLongWithBoundaryBias(1, 5), Scope.GLOBAL, Scope.SESSION),//
        //PERFORMANCE_SCHEMA("performance_schema", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_ACCOUNTS_SIZE("performance_schema_accounts_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_DIGESTS_SIZE("performance_schema_digests_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_STAGES_HISTORY_LONG_SIZE("performance_schema_events_stages_history_long_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_EVENTS_STAGES_HISTORY_SIZE("performance_schema_events_stages_history_size", (r) -> r.getLongWithBoundaryBias(-1, 1024), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_STATEMENTS_HISTORY_LONG_SIZE("performance_schema_events_statements_history_long_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_STATEMENTS_HISTORY_SIZE("performance_schema_events_statements_history_size", (r) -> r.getLongWithBoundaryBias(-1, 1024), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_TRANSACTIONS_HISTORY_LONG_SIZE("performance_schema_events_transactions_history_long_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_TRANSACTIONS_HISTORY_SIZE("performance_schema_events_transactions_history_size", (r) -> r.getLongWithBoundaryBias(-1, 1024), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_WAITS_HISTORY_LONG_SIZE("performance_schema_events_waits_history_long_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_EVENTS_WAITS_HISTORY_SIZE("performance_schema_events_waits_history_size", (r) -> r.getLongWithBoundaryBias(-1, 1024), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_HOSTS_SIZE("performance_schema_hosts_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_COND_CLASSES("performance_schema_max_cond_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_COND_INSTANCES("performance_schema_max_cond_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_MAX_DIGEST_LENGTH("performance_schema_max_digest_length", (r) -> r.getLongWithBoundaryBias(0, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_FILE_CLASSES("performance_schema_max_file_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_FILE_HANDLES("performance_schema_max_file_handles", (r) -> r.getLongWithBoundaryBias(-1, 32768), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_FILE_INSTANCES("performance_schema_max_file_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_INDEX_STAT("performance_schema_max_index_stat", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_MEMORY_CLASSES("performance_schema_max_memory_classes", (r) -> r.getLongWithBoundaryBias(0, 1024), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_MAX_METADATA_LOCKS("performance_schema_max_metadata_locks", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_MUTEX_CLASSES("performance_schema_max_mutex_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_MUTEX_INSTANCES("performance_schema_max_mutex_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_PREPARED_STATEMENTS_INSTANCES("performance_schema_max_prepared_statements_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_PROGRAM_INSTANCES("performance_schema_max_program_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_RWLOCK_CLASSES("performance_schema_max_rwlock_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_RWLOCK_INSTANCES("performance_schema_max_rwlock_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_SOCKET_CLASSES("performance_schema_max_socket_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_SOCKET_INSTANCES("performance_schema_max_socket_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_SQL_TEXT_LENGTH("performance_schema_max_sql_text_length", (r) -> r.getLongWithBoundaryBias(0, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_STAGE_CLASSES("performance_schema_max_stage_classes", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_MAX_STATEMENT_CLASSES("performance_schema_max_statement_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_MAX_STATEMENT_STACK("performance_schema_max_statement_stack", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_MAX_TABLE_HANDLES("performance_schema_max_table_handles", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_TABLE_INSTANCES("performance_schema_max_table_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_TABLE_LOCK_STAT("performance_schema_max_table_lock_stat", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_THREAD_CLASSES("performance_schema_max_thread_classes", (r) -> r.getLongWithBoundaryBias(0, 256), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_MAX_THREAD_INSTANCES("performance_schema_max_thread_instances", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_SESSION_CONNECT_ATTRS_SIZE("performance_schema_session_connect_attrs_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_SETUP_ACTORS_SIZE("performance_schema_setup_actors_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PERFORMANCE_SCHEMA_SETUP_OBJECTS_SIZE("performance_schema_setup_objects_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL),//
//        PERFORMANCE_SCHEMA_USERS_SIZE("performance_schema_users_size", (r) -> r.getLongWithBoundaryBias(-1, 1048576), Scope.GLOBAL), //
//        PID_FILE("pid_file", (r) -> r.getString(), Scope.GLOBAL), //
//        PLUGIN_DIR("plugin_dir", (r) -> r.getString(), Scope.GLOBAL), //
        //PLUGIN_MATURITY("plugin_maturity", (r) -> Randomly.fromOptions("unknown", "experimental", "alpha", "beta", "gamma", "stable"), Scope.GLOBAL), //
        //PORT("port", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL), //
        PRELOAD_BUFFER_SIZE("preload_buffer_size", (r) -> r.getLongWithBoundaryBias(1024, 1073741824), Scope.GLOBAL, Scope.SESSION),//
        PROFILING("profiling", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        PROFILING_HISTORY_SIZE("profiling_history_size", (r) -> r.getLongWithBoundaryBias(0, 100), Scope.GLOBAL, Scope.SESSION), //
        PROGRESS_REPORT_TIME("progress_report_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL, Scope.SESSION), //
        //PROTOCOL_VERSION("protocol_version", (r) -> r.getLongWithBoundaryBias(10, 10), Scope.GLOBAL), //
//        PROXY_PROTOCOL_NETWORKS("proxy_protocol_networks", (r) -> r.getString(), Scope.GLOBAL), //
//        PROXY_USER("proxy_user", (r) -> r.getString(), Scope.SESSION), //
        PSEUDO_SLAVE_MODE("pseudo_slave_mode", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION),//
        PSEUDO_THREAD_ID("pseudo_thread_id", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
        QUERY_ALLOC_BLOCK_SIZE("query_alloc_block_size", (r) -> r.getLongWithBoundaryBias(1024, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        QUERY_CACHE_LIMIT("query_cache_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        QUERY_CACHE_MIN_RES_UNIT("query_cache_min_res_unit", (r) -> r.getLongWithBoundaryBias(512, Long.MAX_VALUE), Scope.GLOBAL), //
        QUERY_CACHE_SIZE("query_cache_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        QUERY_CACHE_STRIP_COMMENTS("query_cache_strip_comments", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        QUERY_CACHE_TYPE("query_cache_type", (r) -> Randomly.fromOptions("OFF", "ON", "DEMAND"), Scope.GLOBAL), //
        QUERY_CACHE_WLOCK_INVALIDATE("query_cache_wlock_invalidate", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        QUERY_PREALLOC_SIZE("query_prealloc_size", (r) -> r.getLongWithBoundaryBias(8192, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
//        RAND_SEED1("rand_seed1", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION), //
//        RAND_SEED2("rand_seed2", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.SESSION),//
        RANGE_ALLOC_BLOCK_SIZE("range_alloc_block_size", (r) -> r.getLongWithBoundaryBias(4096, 4294967295L), Scope.GLOBAL, Scope.SESSION), //
        READ_BINLOG_SPEED_LIMIT("read_binlog_speed_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        READ_BUFFER_SIZE("read_buffer_size", (r) -> r.getLongWithBoundaryBias(8192, 2147479552), Scope.GLOBAL, Scope.SESSION), //
        READ_ONLY("read_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        READ_RND_BUFFER_SIZE("read_rnd_buffer_size", (r) -> r.getLongWithBoundaryBias(1, 2147479552), Scope.GLOBAL, Scope.SESSION), //
//        RELAY_LOG("relay_log", (r) -> r.getString(), Scope.GLOBAL), //
//        RELAY_LOG_BASENAME("relay_log_basename", (r) -> r.getString(), Scope.GLOBAL), //
//        RELAY_LOG_INDEX("relay_log_index", (r) -> r.getString(), Scope.GLOBAL), //
//        RELAY_LOG_INFO_FILE("relay_log_info_file", (r) -> r.getString(), Scope.GLOBAL), //
        RELAY_LOG_PURGE("relay_log_purge", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        RELAY_LOG_RECOVERY("relay_log_recovery", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //RELAY_LOG_SPACE_LIMIT("relay_log_space_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        //REPLICATE_ANNOTATE_ROW_EVENTS("replicate_annotate_row_events", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        REPLICATE_DO_DB("replicate_do_db", (r) -> r.getString(), Scope.GLOBAL), //
//        REPLICATE_DO_TABLE("replicate_do_table", (r) -> r.getString(), Scope.GLOBAL), //
       // REPLICATE_EVENTS_MARKED_FOR_SKIP("replicate_events_marked_for_skip", (r) -> Randomly.fromOptions("replicate", "filter"), Scope.GLOBAL), //
//        REPLICATE_IGNORE_DB("replicate_ignore_db", (r) -> r.getString(), Scope.GLOBAL), //
//        REPLICATE_IGNORE_TABLE("replicate_ignore_table", (r) -> r.getString(), Scope.GLOBAL), //
//        REPLICATE_WILD_DO_TABLE("replicate_wild_do_table", (r) -> r.getString(), Scope.GLOBAL), //
//        REPLICATE_WILD_IGNORE_TABLE("replicate_wild_ignore_table", (r) -> r.getString(), Scope.GLOBAL), //
        //REPORT_HOST("report_host", (r) -> r.getString(), Scope.GLOBAL), //
//        REPORT_PASSWORD("report_password", (r) -> r.getString(), Scope.GLOBAL),//
       // REPORT_PORT("report_port", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.GLOBAL), //
        //REPORT_USER("report_user", (r) -> r.getString(), Scope.GLOBAL), //
        REQUIRE_SECURE_TRANSPORT("require_secure_transport", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        ROWID_MERGE_BUFF_SIZE("rowid_merge_buff_size", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        RPL_SEMI_SYNC_MASTER_ENABLED("rpl_semi_sync_master_enabled", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        RPL_SEMI_SYNC_MASTER_TIMEOUT("rpl_semi_sync_master_timeout", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL), //
        RPL_SEMI_SYNC_MASTER_TRACE_LEVEL("rpl_semi_sync_master_trace_level", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL), //
        RPL_SEMI_SYNC_MASTER_WAIT_NO_SLAVE("rpl_semi_sync_master_wait_no_slave", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        RPL_SEMI_SYNC_MASTER_WAIT_POINT("rpl_semi_sync_master_wait_point", (r) -> Randomly.fromOptions("AFTER_SYNC", "AFTER_COMMIT"), Scope.GLOBAL), //
        RPL_SEMI_SYNC_SLAVE_DELAY_MASTER("rpl_semi_sync_slave_delay_master", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        RPL_SEMI_SYNC_SLAVE_ENABLED("rpl_semi_sync_slave_enabled", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        RPL_SEMI_SYNC_SLAVE_KILL_CONN_TIMEOUT("rpl_semi_sync_slave_kill_conn_timeout", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        RPL_SEMI_SYNC_SLAVE_TRACE_LEVEL("rpl_semi_sync_slave_trace_level", (r) -> r.getLongWithBoundaryBias(0, 255), Scope.GLOBAL), //
        SECURE_AUTH("secure_auth", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        // SECURE_FILE_PRIV("secure_file_priv", (r) -> r.getString(), Scope.GLOBAL), //
        //SECURE_TIMESTAMP("secure_timestamp", (r) -> Randomly.fromOptions("NO", "YES", "SUPER"), Scope.GLOBAL), //
        //SERVER_ID("server_id", (r) -> r.getLongWithBoundaryBias(1, 4294967295L), Scope.GLOBAL), //
        //SERVER_UID("server_uid", (r) -> r.getString(), Scope.GLOBAL), //
        SESSION_TRACK_SCHEMA("session_track_schema", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SESSION_TRACK_STATE_CHANGE("session_track_state_change", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        // SESSION_TRACK_SYSTEM_VARIABLES("session_track_system_variables", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        SESSION_TRACK_TRANSACTION_INFO("session_track_transaction_info", (r) -> Randomly.fromOptions("OFF", "STATE", "CHARACTERISTICS"), Scope.GLOBAL, Scope.SESSION), //
        //SKIP_EXTERNAL_LOCKING("skip_external_locking", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //SKIP_NAME_RESOLVE("skip_name_resolve", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        //SKIP_NETWORKING("skip_networking", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        SKIP_PARALLEL_REPLICATION("skip_parallel_replication", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        SKIP_REPLICATION("skip_replication", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        //SKIP_SHOW_DATABASE("skip_show_database", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL),//
        SLAVE_COMPRESSED_PROTOCOL("slave_compressed_protocol", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        SLAVE_DDL_EXEC_MODE("slave_ddl_exec_mode", (r) -> Randomly.fromOptions("IDEMPOTENT", "STRICT"), Scope.GLOBAL), //
        SLAVE_DOMAIN_PARALLEL_THREADS("slave_domain_parallel_threads", (r) -> r.getLongWithBoundaryBias(0, 1024), Scope.GLOBAL), //
//        SLAVE_EXEC_MODE("slave_exec_mode", (r) -> Randomly.fromOptions("STRICT", "IDEMPOTENT"), Scope.GLOBAL), //
//        //SLAVE_LOAD_TMPDIR("slave_load_tmpdir", (r) -> r.getString(), Scope.GLOBAL), //
//        SLAVE_MAX_ALLOWED_PACKET("slave_max_allowed_packet", (r) -> r.getLongWithBoundaryBias(1024, 1073741824), Scope.GLOBAL), //
//        SLAVE_NET_TIMEOUT("slave_net_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL), //
//        SLAVE_PARALLEL_MAX_QUEUED("slave_parallel_max_queued", (r) -> r.getLongWithBoundaryBias(0, 2147483647), Scope.GLOBAL), //
//        SLAVE_PARALLEL_MODE("slave_parallel_mode", (r) -> Randomly.fromOptions("none", "minimal", "conservative", "optimistic", "aggressive"), Scope.GLOBAL), //
//        SLAVE_PARALLEL_THREADS("slave_parallel_threads", (r) -> r.getLongWithBoundaryBias(0, 1024), Scope.GLOBAL), //
//        SLAVE_PARALLEL_WORKERS("slave_parallel_workers", (r) -> r.getLongWithBoundaryBias(0, 1024), Scope.GLOBAL), //
//        SLAVE_RUN_TRIGGERS_FOR_RBR("slave_run_triggers_for_rbr", (r) -> Randomly.fromOptions("NO", "YES", "LOGGING"), Scope.GLOBAL), //
//        SLAVE_SKIP_ERRORS("slave_skip_errors", (r) -> r.getString(), Scope.GLOBAL), //
//        SLAVE_SQL_VERIFY_CHECKSUM("slave_sql_verify_checksum", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        SLAVE_TRANSACTION_RETRIES("slave_transaction_retries", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
//        SLAVE_TRANSACTION_RETRY_ERRORS("slave_transaction_retry_errors", (r) -> r.getString(), Scope.GLOBAL), //
//        SLAVE_TRANSACTION_RETRY_INTERVAL("slave_transaction_retry_interval", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        //SLAVE_TYPE_CONVERSIONS("slave_type_conversions", (r) -> r.getString(), Scope.GLOBAL), //
        SLOW_LAUNCH_TIME("slow_launch_time", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        SLOW_QUERY_LOG("slow_query_log", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
//        SLOW_QUERY_LOG_FILE("slow_query_log_file", (r) -> r.getString(), Scope.GLOBAL),//
//        SOCKET("socket", (r) -> r.getString(), Scope.GLOBAL), //
        SORT_BUFFER_SIZE("sort_buffer_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        SQL_AUTO_IS_NULL("sql_auto_is_null", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_BIG_SELECTS("sql_big_selects", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_BUFFER_RESULT("sql_buffer_result", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_IF_EXISTS("sql_if_exists", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_LOG_BIN("sql_log_bin", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        SQL_LOG_OFF("sql_log_off", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        SQL_MODE("sql_mode", (r) -> {
            List<String> modes = Arrays.asList(
                    "ALLOW_INVALID_DATES", "ANSI_QUOTES", "ERROR_FOR_DIVISION_BY_ZERO",
                    "IGNORE_SPACE", "NO_AUTO_VALUE_ON_ZERO", "NO_BACKSLASH_ESCAPES",
                    "NO_ENGINE_SUBSTITUTION",
                    "ONLY_FULL_GROUP_BY", "PIPES_AS_CONCAT", "STRICT_ALL_TABLES",
                    "STRICT_TRANS_TABLES"
            );
            return "'"+String.join(",", Randomly.nonEmptySubset(modes))+"'";
        }, Scope.GLOBAL, Scope.SESSION), //
        SQL_NOTES("sql_notes", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION),//
        SQL_QUOTE_SHOW_CREATE("sql_quote_show_create", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_SAFE_UPDATES("sql_safe_updates", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        SQL_SELECT_LIMIT("sql_select_limit", (r) -> r.getLongWithBoundaryBias(0, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        SQL_SLAVE_SKIP_COUNTER("sql_slave_skip_counter", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SQL_WARNINGS("sql_warnings", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
//        SSL_CA("ssl_ca", (r) -> r.getString(), Scope.GLOBAL), //
//        SSL_CAPATH("ssl_capath", (r) -> r.getString(), Scope.GLOBAL), //
//        SSL_CERT("ssl_cert", (r) -> r.getString(), Scope.GLOBAL), //
//        SSL_CIPHER("ssl_cipher", (r) -> r.getString(), Scope.GLOBAL), //
//        SSL_CRL("ssl_crl", (r) -> r.getString(), Scope.GLOBAL), //
//        SSL_CRLPATH("ssl_crlpath", (r) -> r.getString(), Scope.GLOBAL), //
//        SSL_KEY("ssl_key", (r) -> r.getString(), Scope.GLOBAL), //
        STANDARD_COMPLIANT_CTE("standard_compliant_cte", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        STORAGE_ENGINE("storage_engine", (r) -> Randomly.fromOptions("InnoDB", "MyISAM", "Aria", "Memory"), Scope.GLOBAL, Scope.SESSION), //
        STORED_PROGRAM_CACHE("stored_program_cache", (r) -> r.getLongWithBoundaryBias(16, 524288), Scope.GLOBAL), //
        SYNC_BINLOG("sync_binlog", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_FRM("sync_frm", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        SYNC_MASTER_INFO("sync_master_info", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_RELAY_LOG("sync_relay_log", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        SYNC_RELAY_LOG_INFO("sync_relay_log_info", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        //SYSTEM_TIME_ZONE("system_time_zone", (r) -> r.getString(), Scope.GLOBAL), //
        SYSTEM_VERSIONING_ALTER_HISTORY("system_versioning_alter_history", (r) -> Randomly.fromOptions("ERROR", "KEEP"), Scope.GLOBAL), //
        //SYSTEM_VERSIONING_ASOF("system_versioning_asof", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        TABLE_DEFINITION_CACHE("table_definition_cache", (r) -> r.getLongWithBoundaryBias(400, 2097152), Scope.GLOBAL), //
        TABLE_OPEN_CACHE("table_open_cache", (r) -> r.getLongWithBoundaryBias(1, 2097152), Scope.GLOBAL), //
        //TABLE_OPEN_CACHE_INSTANCES("table_open_cache_instances", (r) -> r.getLongWithBoundaryBias(1, 64), Scope.GLOBAL), //
        TCP_KEEPALIVE_INTERVAL("tcp_keepalive_interval", (r) -> r.getLongWithBoundaryBias(0, 31536000), Scope.GLOBAL), //
        TCP_KEEPALIVE_PROBES("tcp_keepalive_probes", (r) -> r.getLongWithBoundaryBias(0, 1000), Scope.GLOBAL), //
        TCP_KEEPALIVE_TIME("tcp_keepalive_time", (r) -> r.getLongWithBoundaryBias(0, 7200), Scope.GLOBAL), //
        TCP_NODELAY("tcp_nodelay", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.SESSION), //
        THREAD_CACHE_SIZE("thread_cache_size", (r) -> r.getLongWithBoundaryBias(0, 16384), Scope.GLOBAL), //
        //THREAD_HANDLING("thread_handling", (r) -> Randomly.fromOptions("one-thread-per-connection", "pool-of-threads", "no-threads"), Scope.GLOBAL),//
        THREAD_POOL_DEDICATED_LISTENER("thread_pool_dedicated_listener", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        THREAD_POOL_EXACT_STATS("thread_pool_exact_stats", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        THREAD_POOL_IDLE_TIMEOUT("thread_pool_idle_timeout", (r) -> r.getLongWithBoundaryBias(1, 65535), Scope.GLOBAL), //
        THREAD_POOL_MAX_THREADS("thread_pool_max_threads", (r) -> r.getLongWithBoundaryBias(1, 65536), Scope.GLOBAL), //
        THREAD_POOL_OVERSUBSCRIBE("thread_pool_oversubscribe", (r) -> r.getLongWithBoundaryBias(1, 1000), Scope.GLOBAL), //
        THREAD_POOL_PRIORITY("thread_pool_priority", (r) -> Randomly.fromOptions("high", "low", "auto"), Scope.GLOBAL, Scope.SESSION), //
        THREAD_POOL_PRIO_KICKUP_TIMER("thread_pool_prio_kickup_timer", (r) -> r.getLongWithBoundaryBias(0, 4294967295L), Scope.GLOBAL), //
        THREAD_POOL_SIZE("thread_pool_size", (r) -> r.getLongWithBoundaryBias(1, 128), Scope.GLOBAL), //
        THREAD_POOL_STALL_LIMIT("thread_pool_stall_limit", (r) -> r.getLongWithBoundaryBias(10, 60000), Scope.GLOBAL), //
        //THREAD_STACK("thread_stack", (r) -> r.getLongWithBoundaryBias(131072, Long.MAX_VALUE), Scope.GLOBAL), //
        TIMESTAMP("timestamp", (r) -> r.getLongWithBoundaryBias(0, 2147483647), Scope.SESSION), //
//        TIME_FORMAT("time_format", (r) -> r.getString(), Scope.GLOBAL),//
//        TIME_ZONE("time_zone", (r) -> r.getString(), Scope.GLOBAL, Scope.SESSION), //
        //TLS_VERSION("tls_version", (r) -> r.getString(), Scope.GLOBAL), //
        TMP_DISK_TABLE_SIZE("tmp_disk_table_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        TMP_MEMORY_TABLE_SIZE("tmp_memory_table_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        TMP_TABLE_SIZE("tmp_table_size", (r) -> r.getLongWithBoundaryBias(1024, Long.MAX_VALUE), Scope.GLOBAL, Scope.SESSION), //
        TRANSACTION_ALLOC_BLOCK_SIZE("transaction_alloc_block_size", (r) -> r.getLongWithBoundaryBias(1024, 131072), Scope.GLOBAL, Scope.SESSION), //
        TRANSACTION_PREALLOC_SIZE("transaction_prealloc_size", (r) -> r.getLongWithBoundaryBias(1024, 131072), Scope.GLOBAL, Scope.SESSION),//
        TX_ISOLATION("tx_isolation", (r) -> Randomly.fromOptions("'READ-UNCOMMITTED'", "'READ-COMMITTED'", "'REPEATABLE-READ'", "'SERIALIZABLE'"), Scope.GLOBAL, Scope.SESSION), //
        TX_READ_ONLY("tx_read_only", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        UNIQUE_CHECKS("unique_checks", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL, Scope.SESSION), //
        UPDATABLE_VIEWS_WITH_LIMIT("updatable_views_with_limit", (r) -> Randomly.fromOptions("YES", "NO"), Scope.GLOBAL, Scope.SESSION), //
        USERSTAT("userstat", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        USE_STAT_TABLES("use_stat_tables", (r) -> Randomly.fromOptions("NEVER", "COMPLEMENTARY", "PREFERABLY"), Scope.GLOBAL, Scope.SESSION), //
//        VERSION("version", (r) -> r.getString(), Scope.GLOBAL), //
//        VERSION_COMMENT("version_comment", (r) -> r.getString(), Scope.GLOBAL), //
//        VERSION_COMPILE_MACHINE("version_compile_machine", (r) -> r.getString(), Scope.GLOBAL), //
//        VERSION_COMPILE_OS("version_compile_os", (r) -> r.getString(), Scope.GLOBAL), //
//        VERSION_MALLOC_LIBRARY("version_malloc_library", (r) -> r.getString(), Scope.GLOBAL),//
//        VERSION_SOURCE_REVISION("version_source_revision", (r) -> r.getString(), Scope.GLOBAL), //
//        VERSION_SSL_LIBRARY("version_ssl_library", (r) -> r.getString(), Scope.GLOBAL), //
        WAIT_TIMEOUT("wait_timeout", (r) -> r.getLongWithBoundaryBias(1, 31536000), Scope.GLOBAL, Scope.SESSION), //
        // WARNING_COUNT("warning_count", (r) -> r.getLongWithBoundaryBias(0, 65535), Scope.SESSION), //
        WSREP_AUTO_INCREMENT_CONTROL("wsrep_auto_increment_control", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL), //
        WSREP_CERTIFICATION_RULES("wsrep_certification_rules", (r) -> Randomly.fromOptions("strict", "optimized"), Scope.GLOBAL), //
        WSREP_CERTIFY_NONPK("wsrep_certify_nonpk", (r) -> Randomly.fromOptions("OFF", "ON"), Scope.GLOBAL); //

        private final GenericAction delegate;

        Action(String name, Function<Randomly, Object> prod, Scope... scopes) {
            this.delegate = new GenericAction(name, prod, scopes);
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

        /*
         * @see https://dev.mysql.com/doc/refman/8.0/en/switchable-optimizations.html
         */

        private static String getOptimizerSwitchConfiguration(Randomly r) {
            StringBuilder sb = new StringBuilder();
            sb.append("'");
            String[] options = { "condition_pushdown_for_derived", "condition_pushdown_for_subquery",
                    "condition_pushdown_from_having", "derived_merge", "derived_with_keys", "exists_to_in",
                    "extended_keys", "firstmatch", "index_condition_pushdown", "hash_join_cardinality", "index_merge",
                    "index_merge_intersection", "index_merge_sort_intersection", "index_merge_sort_union",
                    "index_merge_union", "in_to_exists", "join_cache_bka", "join_cache_hashed",
                    "join_cache_incremental", "loosescan", "materialization", "mrr", "mrr_cost_based", "mrr_sort_keys",
                    "not_null_range_scan", "optimize_join_buffer_size", "orderby_uses_equalities",
                    "outer_join_with_cache", "partial_match_rowid_merge", "partial_match_table_scan", "rowid_filter",
                    "semijoin", "semijoin_with_cache", "split_materialized", "subquery_cache", "table_elimination" };
            List<String> optionSubset =  Randomly.nonEmptySubset(options);
            sb.append(optionSubset.stream().map(s -> s + "=" + Randomly.fromOptions("on", "off"))
                    .collect(Collectors.joining(",")));
            sb.append("'");
            return sb.toString();
        }


    }




    public static List<SQLQueryAdapter> getAllOptimizer(MariaDBGlobalState globalState) {
        List<SQLQueryAdapter> result = new ArrayList<>();
        String[] options = { "condition_pushdown_for_derived", "condition_pushdown_for_subquery",
                "condition_pushdown_from_having", "derived_merge", "derived_with_keys", "exists_to_in", "extended_keys",
                "firstmatch", "index_condition_pushdown", "hash_join_cardinality", "index_merge",
                "index_merge_intersection", "index_merge_sort_intersection", "index_merge_sort_union",
                "index_merge_union", "in_to_exists", "join_cache_bka", "join_cache_hashed", "join_cache_incremental",
                "loosescan", "materialization", "mrr", "mrr_cost_based", "mrr_sort_keys", "not_null_range_scan",
                "optimize_join_buffer_size", "orderby_uses_equalities", "outer_join_with_cache",
                "partial_match_rowid_merge", "partial_match_table_scan", "rowid_filter", "semijoin",
                "semijoin_with_cache", "split_materialized", "subquery_cache", "table_elimination" };
        List<String> availableOptions = new ArrayList<>(Arrays.asList(options));
        if (MariaDBBugs.bug21058) {
            availableOptions.remove("in_to_exists"); // https://jira.mariadb.org/browse/MDEV-21058
        }
        if (MariaDBBugs.bug32076) {
            availableOptions.remove("not_null_range_scan"); // https://jira.mariadb.org/browse/MDEV-32076
        }
        if (MariaDBBugs.bug32099) {
            availableOptions.remove("optimize_join_buffer_size"); // https://jira.mariadb.org/browse/MDEV-32099
        }
        if (MariaDBBugs.bug32105) {
            availableOptions.remove("join_cache_hashed"); // https://jira.mariadb.org/browse/MDEV-32105
        }
        if (MariaDBBugs.bug32106) {
            availableOptions.remove("outer_join_with_cache"); // https://jira.mariadb.org/browse/MDEV-32106
        }
        if (MariaDBBugs.bug32107) {
            availableOptions.remove("table_elimination"); // https://jira.mariadb.org/browse/MDEV-32107
        }
        if (MariaDBBugs.bug32108) {
            availableOptions.remove("join_cache_incremental"); // https://jira.mariadb.org/browse/MDEV-32108
        }
        if (MariaDBBugs.bug32143) {
            availableOptions.remove("mrr"); // https://jira.mariadb.org/browse/MDEV-32143
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SET SESSION optimizer_switch = '%s'");

        for (String option : availableOptions) {
            result.add(new SQLQueryAdapter(String.format(sb.toString(), option + "=on"), ExpectedErrors.from(
                    "At least one of the 'in_to_exists' or 'materialization' optimizer_switch flags must be 'on'")));
            result.add(new SQLQueryAdapter(String.format(sb.toString(), option + "=off"), ExpectedErrors.from(
                    "At least one of the 'in_to_exists' or 'materialization' optimizer_switch flags must be 'on'")));
            result.add(new SQLQueryAdapter(String.format(sb.toString(), option + "=default"), ExpectedErrors.from(
                    "At least one of the 'in_to_exists' or 'materialization' optimizer_switch flags must be 'on'")));
        }

        return result;
    }

}
