#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    use jni::JNIEnv;
    use jni::objects::{JClass, JObject, JString, JValue};
    use jni::sys::{jint, jobjectArray};

    use dictp::{Database, Dict, Strategy, commands::Command, responses::Definition};

    fn connect(env: &JNIEnv, server: JString, port: jint) -> Dict {
        let server: String = env.get_string(server).unwrap().into();
        let port: u16 = port as u16;

        Dict::connect(&server, port).unwrap()
    }

    fn define(dict: &mut Dict, cmd: Command) -> Vec<Definition> {
        dict.define(cmd).unwrap().collect::<Vec<Definition>>()
    }

    fn convert_definitions_to_java(env: &JNIEnv, defns: Vec<Definition>) -> jobjectArray {
        let java_definition_class = env.find_class("com/savanto/andict/Definition").unwrap();

        let definitions = env.new_object_array(
            defns.len() as i32,
            java_definition_class,
            JObject::null(),
        ).unwrap();

        for (idx, defn) in defns.iter().enumerate() {
            let database = env.new_string(&defn.database).unwrap();
            let definition = env.new_string(&defn.definition).unwrap();
            let definition = env.new_object(
                java_definition_class,
                "(Ljava/lang/String;Ljava/lang/String;)V",
                &[JValue::Object(database.into()), JValue::Object(definition.into())],
            ).unwrap();
            env.set_object_array_element(definitions, idx as i32, definition).unwrap();
        }

        definitions
    }

    fn convert_entities_to_java(env: &JNIEnv, ents: Vec<(String, String)>) -> jobjectArray {
        let java_entity_class = env.find_class("com/savanto/andict/Entity").unwrap();

        let entities = env.new_object_array(
            ents.len() as i32,
            java_entity_class,
            JObject::null(),
        ).unwrap();

        for (idx, ent) in ents.iter().enumerate() {
            let name = env.new_string(&ent.0).unwrap();
            let desc = env.new_string(&ent.1).unwrap();
            let ent = env.new_object(
                java_entity_class,
                "(Ljava/lang/String;Ljava/lang/String;)V",
                &[JValue::Object(name.into()), JValue::Object(desc.into())],
            ).unwrap();
            env.set_object_array_element(entities, idx as i32, ent).unwrap();
        }

        entities
    }

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_define(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
        database: JString,
        word: JString,
    ) -> jobjectArray {
        let database: String = env.get_string(database).unwrap().into();
        let database: Database = database.parse().unwrap();
        let word: String = env.get_string(word).unwrap().into();
        let cmd = Command::Define(database, word);
        let mut dict: Dict = connect(&env, server, port);
        let defns: Vec<Definition> = define(&mut dict, cmd);

        convert_definitions_to_java(&env, defns)
    }

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_defineWithStrategy(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
        database: JString,
        strategy: JString,
        word: JString,
    ) -> jobjectArray {
        let database: String = env.get_string(database).unwrap().into();
        let database: Database = database.parse().unwrap();
        let strategy: String = env.get_string(strategy).unwrap().into();
        let strategy: Strategy = strategy.parse().unwrap();
        let word: String = env.get_string(word).unwrap().into();
        let cmd = Command::Match(database, strategy, word);
        let mut dict: Dict = connect(&env, server, port);
        let mut defns: Vec<Definition> = Vec::new();
        let matches = dict.r#match(cmd).unwrap();
        for match_ in matches {
            let cmd = Command::Define(match_.database, match_.word);
            defns.extend(define(&mut dict, cmd));
        }

        convert_definitions_to_java(&env, defns)
    }

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_showStrategies(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
    ) -> jobjectArray {
        let mut dict: Dict = connect(&env, server, port);
        let strategies = dict
            .show_strategies()
            .unwrap()
            .map(|strat| (strat.strategy.to_string(), strat.description.to_string()))
            .collect();

        convert_entities_to_java(&env, strategies)
    }

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_showDatabases(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
    ) -> jobjectArray {
        let mut dict: Dict = connect(&env, server, port);
        let databases = dict
            .show_databases()
            .unwrap()
            .map(|db| (db.database.to_string(), db.description.to_string()))
            .collect();

        convert_entities_to_java(&env, databases)
    }
}
