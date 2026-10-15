\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE database_table_comment (
    table_name TEXT PRIMARY KEY,
    description TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO database_table_comment (table_name, description) VALUES
    ('act_evt_log', 'Flowable 流程引擎事件日志表，记录流程运行期间产生的事件及其原始数据。'),
    ('act_ge_bytearray', 'Flowable 通用二进制资源表，保存流程定义、模型、变量和异常堆栈等二进制内容。'),
    ('act_ge_property', 'Flowable 流程引擎属性表，保存引擎版本、数据库版本和内部配置值。'),
    ('act_hi_actinst', 'Flowable 历史活动实例表，记录流程节点的开始、结束和执行结果。'),
    ('act_hi_attachment', 'Flowable 历史附件表，记录流程实例或任务关联的附件信息。'),
    ('act_hi_comment', 'Flowable 历史评论表，记录用户对流程实例或任务的评论和操作消息。'),
    ('act_hi_detail', 'Flowable 历史明细表，记录变量更新、表单属性等细粒度历史数据。'),
    ('act_hi_entitylink', 'Flowable 历史实体关联表，记录流程、任务与其他作用域实体之间的历史关系。'),
    ('act_hi_identitylink', 'Flowable 历史身份关联表，记录用户或用户组与流程、任务的历史参与关系。'),
    ('act_hi_procinst', 'Flowable 历史流程实例表，记录已启动流程实例的生命周期和业务标识。'),
    ('act_hi_taskinst', 'Flowable 历史任务实例表，记录人工任务的生命周期、办理人和状态。'),
    ('act_hi_tsk_log', 'Flowable 历史任务操作日志表，记录任务认领、委派、完成等操作事件。'),
    ('act_hi_varinst', 'Flowable 历史变量实例表，保存流程和任务变量的历史最终值。'),
    ('act_procdef_info', 'Flowable 流程定义扩展信息表，保存流程定义对应的附加 JSON 配置。'),
    ('act_re_deployment', 'Flowable 流程部署表，记录流程资源部署批次及部署时间。'),
    ('act_re_model', 'Flowable 流程模型表，保存设计器模型元数据及其编辑资源引用。'),
    ('act_re_procdef', 'Flowable 流程定义表，保存已部署 BPMN 流程定义的版本和资源信息。'),
    ('act_ru_actinst', 'Flowable 运行时活动实例表，记录当前正在执行的流程节点。'),
    ('act_ru_deadletter_job', 'Flowable 死信作业表，保存重试耗尽或无法继续执行的异步作业。'),
    ('act_ru_entitylink', 'Flowable 运行时实体关联表，记录当前流程、任务与其他作用域实体的关系。'),
    ('act_ru_event_subscr', 'Flowable 运行时事件订阅表，记录消息、信号和补偿等事件的等待订阅。'),
    ('act_ru_execution', 'Flowable 运行时执行实例表，保存流程实例及其执行树的当前状态。'),
    ('act_ru_external_job', 'Flowable 外部工作者作业表，保存等待外部工作者获取并执行的作业。'),
    ('act_ru_history_job', 'Flowable 异步历史作业表，保存等待写入历史数据的后台作业。'),
    ('act_ru_identitylink', 'Flowable 运行时身份关联表，记录当前用户或用户组与流程、任务的参与关系。'),
    ('act_ru_job', 'Flowable 可执行作业表，保存等待作业执行器处理的异步作业。'),
    ('act_ru_suspended_job', 'Flowable 挂起作业表，保存因流程或作业挂起而暂停执行的作业。'),
    ('act_ru_task', 'Flowable 运行时任务表，保存当前待办人工任务及办理状态。'),
    ('act_ru_timer_job', 'Flowable 定时作业表，保存等待到期触发的定时器作业。'),
    ('act_ru_variable', 'Flowable 运行时变量表，保存当前流程、执行实例和任务变量。'),
    ('app_setting', '应用配置表，保存工作台后端的持久化配置项及说明。'),
    ('flw_channel_definition', 'Flowable 事件注册表频道定义表，保存事件接收或发送频道的配置。'),
    ('flw_ev_databasechangelog', 'Flowable 事件模块 Liquibase 变更记录表，记录已执行的数据库变更集。'),
    ('flw_ev_databasechangeloglock', 'Flowable 事件模块 Liquibase 变更锁表，防止多个实例并发执行数据库变更。'),
    ('flw_event_definition', 'Flowable 事件定义表，保存已部署事件模型中的事件定义。'),
    ('flw_event_deployment', 'Flowable 事件部署表，记录事件模型资源的部署批次。'),
    ('flw_event_resource', 'Flowable 事件资源表，保存事件部署包含的资源文件及二进制内容。'),
    ('flw_ru_batch', 'Flowable 批处理主表，保存批量任务的类型、状态和总体执行信息。'),
    ('flw_ru_batch_part', 'Flowable 批处理分片表，保存批量任务拆分后的单个执行部分。'),
    ('flyway_schema_history', 'Flyway 数据库版本记录表，保存应用数据库迁移脚本的执行历史。'),
    ('submission_log', '用户提交日志表，记录工作台用户的代码提交时间、哈希、范围和摘要。'),
    ('workbench_user', '工作台用户表，保存登录账号、密码哈希、角色、启用状态和首次改密状态。'),
    ('workspace_application', '工作台应用表，保存个人应用和合作应用的基本信息、访问地址及运行状态。'),
    ('workspace_application_collaborator', '工作台应用合作人关联表，记录合作应用与参与用户的多对多关系。');

CREATE TEMP TABLE database_column_comment (
    column_name TEXT PRIMARY KEY,
    description TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO database_column_comment (column_name, description) VALUES
    ('act_id_', '流程活动定义标识，用于定位 BPMN 节点。'),
    ('act_inst_id_', '活动实例标识。'),
    ('act_name_', '流程活动显示名称。'),
    ('act_type_', '流程活动的 BPMN 元素类型。'),
    ('action_', '评论或操作的动作类型。'),
    ('active', '是否为启用状态。'),
    ('activity_id_', '事件订阅关联的流程活动标识。'),
    ('adv_handler_cfg_id_', '高级作业处理器配置标识。'),
    ('application_id', '工作台应用唯一标识。'),
    ('application_url', '应用访问地址；为空时工作台提示补充地址。'),
    ('area', '代码提交涉及的功能区域或模块。'),
    ('assignee_', '任务当前办理人用户标识。'),
    ('author', '数据库变更集作者。'),
    ('batch_doc_id_', '批处理输入文档的二进制资源标识。'),
    ('batch_id_', '所属批处理任务标识。'),
    ('business_key_', '流程实例关联的业务主键。'),
    ('business_status_', '流程实例对应业务对象的状态。'),
    ('bytearray_id_', '变量或历史明细关联的二进制资源标识。'),
    ('bytes_', '二进制资源内容。'),
    ('cached_ent_state_', '执行实例缓存的子实体状态位标记。'),
    ('call_proc_inst_id_', '调用活动启动的子流程实例标识。'),
    ('callback_id_', '外部回调对象标识。'),
    ('callback_type_', '外部回调对象类型。'),
    ('category', '工作台应用分类。'),
    ('category_', '流程、任务、部署或事件定义的分类。'),
    ('checksum', '迁移脚本校验和，用于检测脚本内容变更。'),
    ('claim_time_', '任务被认领的时间。'),
    ('claimed_by_', '认领任务的用户标识。'),
    ('comments', '数据库变更集备注。'),
    ('commit_hash', '代码提交哈希。'),
    ('committed_at', '代码提交时间。'),
    ('complete_time_', '批处理或任务完成时间。'),
    ('completed_by_', '完成任务的用户标识。'),
    ('configuration_', '事件订阅配置内容，例如消息名或补偿配置。'),
    ('content_id_', '附件内容对应的二进制资源标识。'),
    ('contexts', 'Liquibase 变更集上下文条件。'),
    ('correlation_id_', '作业关联标识，用于关联同一业务或执行链路。'),
    ('create_time_', '记录创建时间。'),
    ('created_', '事件订阅创建时间。'),
    ('created_at', '记录创建时间。'),
    ('custom_values_id_', '作业自定义值对应的二进制资源标识。'),
    ('data_', '事件或任务日志的扩展数据。'),
    ('dateexecuted', '数据库变更集执行时间。'),
    ('deadletter_job_count_', '执行实例下死信作业数量。'),
    ('delegation_', '任务委派状态。'),
    ('delete_reason_', '流程活动、流程实例或任务的结束/删除原因。'),
    ('deploy_time_', '流程或事件资源部署时间。'),
    ('deployment_id', 'Liquibase 本次更新的部署标识。'),
    ('deployment_id_', '所属流程或事件部署标识。'),
    ('derived_from_', '派生来源部署或流程定义标识。'),
    ('derived_from_root_', '派生链根部署或根流程定义标识。'),
    ('derived_version_', '派生流程定义版本号。'),
    ('description', '记录的业务说明。'),
    ('description_', '流程、任务、频道或事件定义的说明。'),
    ('dgrm_resource_name_', '流程图资源文件名称。'),
    ('double_', '变量的双精度数值。'),
    ('due_date_', '任务到期时间。'),
    ('duedate_', '作业计划执行或到期时间。'),
    ('duration_', '活动、流程或任务持续时长，单位为毫秒。'),
    ('editor_source_extra_value_id_', '模型编辑器附加源数据的二进制资源标识。'),
    ('editor_source_value_id_', '模型编辑器源数据的二进制资源标识。'),
    ('element_id_', '作业对应的 BPMN 元素标识。'),
    ('element_name_', '作业对应的 BPMN 元素名称。'),
    ('end_act_id_', '流程实例结束时所在的活动标识。'),
    ('end_time_', '活动、流程或任务结束时间。'),
    ('engine_version_', '创建部署或流程定义的 Flowable 引擎版本。'),
    ('event_name_', '订阅的事件名称。'),
    ('event_type_', '订阅的事件类型，例如 message、signal 或 compensate。'),
    ('evt_subscr_count_', '执行实例下事件订阅数量。'),
    ('exception_msg_', '作业最近一次执行异常消息。'),
    ('exception_stack_id_', '作业异常堆栈对应的二进制资源标识。'),
    ('exclusive_', '作业是否为排他作业。'),
    ('exectype', 'Liquibase 变更集执行结果类型。'),
    ('execution_id_', '所属流程执行实例标识。'),
    ('execution_time', '迁移脚本执行耗时，单位为毫秒。'),
    ('external_worker_job_count_', '执行实例下外部工作者作业数量。'),
    ('filename', '数据库变更集所在文件路径。'),
    ('form_key_', '任务或流程启动表单键。'),
    ('full_msg_', '评论完整消息的二进制内容。'),
    ('generated_', '二进制资源是否由引擎自动生成。'),
    ('group_id_', '关联的用户组标识。'),
    ('handler_cfg_', '作业处理器配置。'),
    ('handler_type_', '作业处理器类型。'),
    ('has_graphical_notation_', '流程定义是否包含图形坐标信息。'),
    ('has_start_form_key_', '流程定义是否配置启动表单键。'),
    ('hierarchy_type_', '实体关联的层级关系类型。'),
    ('id', '记录主键。'),
    ('id_', 'Flowable 记录唯一标识。'),
    ('id_link_count_', '执行实例或任务下身份关联数量。'),
    ('implementation_', '事件频道的实现类型或适配器标识。'),
    ('in_progress_due_date_', '任务进入处理中状态后的到期时间。'),
    ('in_progress_started_by_', '将任务置为处理中状态的用户标识。'),
    ('in_progress_time_', '任务进入处理中状态的时间。'),
    ('info_json_id_', '流程定义扩展 JSON 对应的二进制资源标识。'),
    ('installed_by', '执行 Flyway 迁移的数据库用户。'),
    ('installed_on', 'Flyway 迁移安装时间。'),
    ('installed_rank', 'Flyway 迁移执行顺序。'),
    ('is_active_', '执行实例是否处于活动状态。'),
    ('is_concurrent_', '执行实例是否为并发执行分支。'),
    ('is_count_enabled_', '是否启用执行实例子实体数量统计。'),
    ('is_event_scope_', '执行实例是否为事件作用域。'),
    ('is_mi_root_', '执行实例是否为多实例根节点。'),
    ('is_processed_', '事件日志是否已被处理。'),
    ('is_scope_', '执行实例是否为流程作用域。'),
    ('job_count_', '执行实例下可执行作业数量。'),
    ('key_', '流程模型、定义、频道或事件定义的业务键。'),
    ('labels', 'Liquibase 变更集标签条件。'),
    ('last_update_time_', '模型最后更新时间。'),
    ('last_updated_time_', '任务或变量最后更新时间。'),
    ('link_type_', '实体或身份关联类型。'),
    ('liquibase', '执行变更集的 Liquibase 版本。'),
    ('lock_exp_time_', '作业锁过期时间。'),
    ('lock_owner_', '当前持有记录或作业锁的执行器标识。'),
    ('lock_time_', '记录被锁定的时间。'),
    ('locked', 'Liquibase 变更锁是否已被占用。'),
    ('lockedby', '持有 Liquibase 变更锁的实例信息。'),
    ('lockgranted', 'Liquibase 变更锁获得时间。'),
    ('log_nr_', '事件日志顺序号。'),
    ('long_', '变量的长整型数值。'),
    ('md5sum', 'Liquibase 变更集内容校验和。'),
    ('message_', '评论或操作的简短消息。'),
    ('meta_info_', '模型或变量的扩展元数据。'),
    ('name', '记录名称。'),
    ('name_', 'Flowable 对象名称或变量名称。'),
    ('orderexecuted', 'Liquibase 变更集执行顺序。'),
    ('owner_', '任务所有者用户标识。'),
    ('owner_username', '工作台应用所有者账号。'),
    ('parent_deployment_id_', '父部署标识。'),
    ('parent_element_id_', '父级 BPMN 元素标识。'),
    ('parent_id_', '父执行实例标识。'),
    ('parent_task_id_', '父任务标识。'),
    ('password_change_required', '是否要求用户下次登录时修改密码。'),
    ('password_hash', '用户登录密码的 BCrypt 哈希值，不存储明文密码。'),
    ('priority_', '任务或作业优先级。'),
    ('proc_def_id_', '流程定义标识。'),
    ('proc_inst_id_', '流程实例标识。'),
    ('process_instance_id_', '作业所属流程实例标识。'),
    ('propagated_stage_inst_id_', '从上层传播的阶段实例标识。'),
    ('ref_scope_definition_id_', '被引用作用域的定义标识。'),
    ('ref_scope_id_', '被引用作用域实例标识。'),
    ('ref_scope_type_', '被引用作用域类型。'),
    ('reference_id_', '流程实例关联的外部对象标识。'),
    ('reference_type_', '流程实例关联的外部对象类型。'),
    ('repeat_', '定时作业重复执行表达式。'),
    ('resource_bytes_', '事件部署资源的二进制内容。'),
    ('resource_name_', '流程或事件定义资源文件名称。'),
    ('result_doc_id_', '批处理分片结果文档的二进制资源标识。'),
    ('retries_', '作业剩余重试次数。'),
    ('rev_', '乐观锁版本号，用于并发更新控制。'),
    ('roles', '用户角色列表，多个角色使用逗号分隔。'),
    ('root_proc_inst_id_', '根流程实例标识。'),
    ('root_scope_id_', '根作用域实例标识。'),
    ('root_scope_type_', '根作用域类型。'),
    ('scope', '应用范围：personal 为个人应用，collaboration 为合作应用。'),
    ('scope_definition_id_', '所属作用域定义标识。'),
    ('scope_definition_key_', '所属作用域定义业务键。'),
    ('scope_id_', '所属作用域实例标识。'),
    ('scope_type_', '所属作用域类型。'),
    ('script', 'Flyway 迁移脚本文件名。'),
    ('search_key2_', '批处理的第二检索键。'),
    ('search_key_', '批处理的主要检索键。'),
    ('setting_key', '应用配置键，业务范围内唯一。'),
    ('setting_value', '应用配置值。'),
    ('start_act_id_', '流程实例启动活动标识。'),
    ('start_time_', '活动、流程或任务开始时间。'),
    ('start_user_id_', '启动流程实例的用户标识。'),
    ('state_', '任务当前生命周期状态。'),
    ('status', '工作台应用状态：running 为运行中，paused 为已暂停。'),
    ('status_', '批处理及分片当前状态。'),
    ('sub_scope_id_', '子作用域实例标识。'),
    ('sub_task_count_', '任务的直接子任务数量。'),
    ('success', 'Flyway 迁移是否执行成功。'),
    ('summary', '代码提交内容摘要。'),
    ('super_exec_', '调用当前流程实例的上级执行实例标识。'),
    ('super_process_instance_id_', '上级流程实例标识。'),
    ('susp_job_count_', '执行实例下挂起作业数量。'),
    ('suspended_by_', '挂起任务的用户标识。'),
    ('suspended_time_', '任务挂起时间。'),
    ('suspension_state_', '流程定义、执行实例或任务的挂起状态代码。'),
    ('tag', 'Liquibase 数据库变更标签。'),
    ('task_count_', '执行实例下运行时任务数量。'),
    ('task_def_id_', '任务定义标识。'),
    ('task_def_key_', '任务定义业务键。'),
    ('task_id_', '关联任务标识。'),
    ('tenant_id_', '多租户标识；为空表示默认租户。'),
    ('text2_', '变量的第二文本值或辅助序列化值。'),
    ('text_', '变量的文本值。'),
    ('time_', '附件、评论或历史明细产生时间。'),
    ('time_stamp_', '事件或任务日志时间戳。'),
    ('timer_job_count_', '执行实例下定时作业数量。'),
    ('transaction_order_', '活动在同一事务中的执行顺序。'),
    ('type', '迁移脚本类型。'),
    ('type_', 'Flowable 对象、日志、附件、关联或作业类型。'),
    ('updated_at', '记录最后更新时间。'),
    ('url_', '附件外部访问地址。'),
    ('user_id_', '关联用户标识。'),
    ('username', '工作台用户登录账号。'),
    ('value_', 'Flowable 引擎属性值。'),
    ('var_count_', '执行实例或任务下运行时变量数量。'),
    ('var_type_', '历史变量类型。'),
    ('version', 'Flyway 迁移版本号。'),
    ('version_', '模型、流程定义、频道或事件定义版本号。'),
    ('visibility', '应用可见性：shared 为共享，exclusive 为独享。');

CREATE TEMP TABLE database_column_comment_override (
    table_name TEXT NOT NULL,
    column_name TEXT NOT NULL,
    description TEXT NOT NULL,
    PRIMARY KEY (table_name, column_name)
) ON COMMIT DROP;

INSERT INTO database_column_comment_override (table_name, column_name, description) VALUES
    ('app_setting', 'id', '应用配置记录自增主键。'),
    ('app_setting', 'description', '配置项用途说明。'),
    ('workbench_user', 'username', '工作台登录账号，同时作为用户业务主键。'),
    ('workbench_user', 'created_at', '用户创建时间。'),
    ('workbench_user', 'updated_at', '用户资料或密码最后更新时间。'),
    ('workspace_application', 'id', '工作台应用唯一标识。'),
    ('workspace_application', 'name', '应用显示名称。'),
    ('workspace_application', 'description', '应用功能简介。'),
    ('workspace_application', 'created_at', '应用创建时间。'),
    ('workspace_application', 'updated_at', '应用信息最后更新时间。'),
    ('workspace_application_collaborator', 'username', '合作人账号，关联工作台用户。'),
    ('workspace_application_collaborator', 'created_at', '合作关系创建时间。'),
    ('submission_log', 'id', '提交日志唯一标识。'),
    ('submission_log', 'username', '提交人账号，关联工作台用户。'),
    ('flw_ev_databasechangelog', 'id', 'Liquibase 变更集标识。'),
    ('flw_ev_databasechangelog', 'description', 'Liquibase 对变更集操作类型的描述。'),
    ('flw_ev_databasechangeloglock', 'id', 'Liquibase 锁记录主键。'),
    ('flyway_schema_history', 'description', 'Flyway 迁移描述。');

DO $comment_tables$
DECLARE
    current_table RECORD;
    table_description TEXT;
BEGIN
    FOR current_table IN
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_type = 'BASE TABLE'
        ORDER BY table_name
    LOOP
        SELECT description
        INTO table_description
        FROM database_table_comment
        WHERE table_name = current_table.table_name;

        IF table_description IS NULL THEN
            RAISE EXCEPTION 'Missing table comment mapping for public.%', current_table.table_name;
        END IF;

        EXECUTE format(
            'COMMENT ON TABLE %I.%I IS %L',
            'public',
            current_table.table_name,
            table_description
        );
    END LOOP;
END
$comment_tables$;

DO $comment_columns$
DECLARE
    current_column RECORD;
    column_description TEXT;
BEGIN
    FOR current_column IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
        ORDER BY table_name, ordinal_position
    LOOP
        SELECT COALESCE(column_override.description, column_default.description)
        INTO column_description
        FROM (SELECT 1) AS anchor
        LEFT JOIN database_column_comment_override column_override
          ON column_override.table_name = current_column.table_name
         AND column_override.column_name = current_column.column_name
        LEFT JOIN database_column_comment column_default
          ON column_default.column_name = current_column.column_name;

        IF column_description IS NULL THEN
            RAISE EXCEPTION 'Missing column comment mapping for public.%.%',
                current_column.table_name,
                current_column.column_name;
        END IF;

        EXECUTE format(
            'COMMENT ON COLUMN %I.%I.%I IS %L',
            'public',
            current_column.table_name,
            current_column.column_name,
            column_description
        );
    END LOOP;
END
$comment_columns$;

DO $verify_comments$
DECLARE
    table_total INTEGER;
    table_commented INTEGER;
    column_total INTEGER;
    column_commented INTEGER;
BEGIN
    SELECT COUNT(*), COUNT(obj_description(class.oid, 'pg_class'))
    INTO table_total, table_commented
    FROM pg_class class
    JOIN pg_namespace namespace ON namespace.oid = class.relnamespace
    WHERE namespace.nspname = 'public'
      AND class.relkind IN ('r', 'p');

    SELECT COUNT(*), COUNT(col_description(class.oid, attribute.attnum))
    INTO column_total, column_commented
    FROM pg_attribute attribute
    JOIN pg_class class ON class.oid = attribute.attrelid
    JOIN pg_namespace namespace ON namespace.oid = class.relnamespace
    WHERE namespace.nspname = 'public'
      AND class.relkind IN ('r', 'p')
      AND attribute.attnum > 0
      AND NOT attribute.attisdropped;

    IF table_total <> table_commented THEN
        RAISE EXCEPTION 'Table comment coverage incomplete: % of %', table_commented, table_total;
    END IF;

    IF column_total <> column_commented THEN
        RAISE EXCEPTION 'Column comment coverage incomplete: % of %', column_commented, column_total;
    END IF;
END
$verify_comments$;

SELECT
    (SELECT COUNT(*)
     FROM pg_class class
     JOIN pg_namespace namespace ON namespace.oid = class.relnamespace
     WHERE namespace.nspname = 'public'
       AND class.relkind IN ('r', 'p')
       AND obj_description(class.oid, 'pg_class') IS NOT NULL) AS commented_tables,
    (SELECT COUNT(*)
     FROM pg_attribute attribute
     JOIN pg_class class ON class.oid = attribute.attrelid
     JOIN pg_namespace namespace ON namespace.oid = class.relnamespace
     WHERE namespace.nspname = 'public'
       AND class.relkind IN ('r', 'p')
       AND attribute.attnum > 0
       AND NOT attribute.attisdropped
       AND col_description(class.oid, attribute.attnum) IS NOT NULL) AS commented_columns;

COMMIT;
