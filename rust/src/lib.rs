#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    use jni::JNIEnv;
    use jni::objects::{JClass, JObject, JString, JValue};
    use jni::sys::{jint, jobjectArray};

    use dictp::{Database, Dict, Strategy, commands::{Define, Match}, responses::Definition};

    fn connect(env: &JNIEnv, server: JString, port: jint) -> Dict {
        let server: String = env.get_string(server).unwrap().into();
        let port: u16 = port as u16;

        Dict::connect(&server, port).unwrap()
    }

    fn define(dict: &mut Dict, cmd: Define) -> Vec<Definition> {
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
        let cmd = Define::new(database, word);
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
        let cmd = Match::new(database, strategy, word);
        let mut dict: Dict = connect(&env, server, port);
        let mut defns: Vec<Definition> = Vec::new();
        let matches = dict.r#match(cmd).unwrap();
        for match_ in matches {
            let cmd = Define::new(match_.database, match_.word);
            defns.extend(define(&mut dict, cmd));
        }

        convert_definitions_to_java(&env, defns)
    }
}
