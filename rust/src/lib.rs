#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    use std::collections::HashSet;
    use std::io;

    use jni::JNIEnv;
    use jni::objects::{JClass, JObject, JString, JValue};
    use jni::sys::{jint, jobjectArray};

    use dictp::{Database, Dict, Strategy, commands::Command, responses::{Definition, Match}};

    fn connect(env: &JNIEnv, server: JString, port: jint) -> io::Result<Dict> {
        let server: String = env.get_string(server).unwrap().into();
        let port: u16 = port as u16;

        Dict::connect(&server, port)
    }

    fn define(dict: &mut Dict, cmd: Command) -> io::Result<Vec<Definition>> {
        dict.define(cmd).map(|defns| defns.collect::<Vec<Definition>>())
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
        let defns = connect(&env, server, port)
            .and_then(|mut dict| define(&mut dict, Command::Define(database, word)))
            .map(|defns| convert_definitions_to_java(&env, defns));

        if let Ok(defns) = defns {
            defns
        } else {
            JObject::null().into_inner()
        }
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

        let defns = connect(&env, server, port)
            .and_then(|mut dict| {
                dict.r#match(Command::Match(database, strategy, word))
                    .map(|matches| matches.collect::<Vec<Match>>())
                    .map(|matches| {
                        let mut definitions: Vec<Definition> = Vec::new();
                        let mut unique_matches = HashSet::new();
                        for match_ in matches {
                            if ! unique_matches.contains(&match_) {
                                unique_matches.insert(match_.clone());
                                let cmd = Command::Define(match_.database, match_.word);
                                if let Ok(defns) = define(&mut dict, cmd) {
                                    definitions.extend(defns);
                                }
                            }
                        }

                        convert_definitions_to_java(&env, definitions)
                    })
            });

        if let Ok(defns) = defns {
            defns
        } else {
            JObject::null().into_inner()
        }
    }

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_showStrategies(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
    ) -> jobjectArray {
        let strategies = connect(&env, server, port)
            .and_then(|mut dict| dict.show_strategies())
            .map(|strategies| {
                strategies
                    .map(|strat| (strat.strategy.to_string(), strat.description.to_string()))
                    .collect::<Vec<(String, String)>>()
            })
            .map(|strategies| convert_entities_to_java(&env, strategies));

        if let Ok(strategies) = strategies {
            strategies
        } else {
            JObject::null().into_inner()
        }
    }

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_showDatabases(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
    ) -> jobjectArray {
        let databases = connect(&env, server, port)
            .and_then(|mut dict| dict.show_databases())
            .map(|databases| {
                databases.map(|db| (db.database.to_string(), db.description.to_string()))
                .collect()
            })
            .map(|databases| convert_entities_to_java(&env, databases));

        if let Ok(databases) = databases {
            databases
        } else {
            JObject::null().into_inner()
        }
    }
}
