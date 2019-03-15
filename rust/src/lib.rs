#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    use jni::JNIEnv;
    use jni::objects::{JClass, JObject, JString, JValue};
    use jni::sys::{jint, jobjectArray};

    use dictp::{Database, Dict, commands::Define, responses::Definition};

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_define(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
        database: JString,
        word: JString,
    ) -> jobjectArray {
        let server: String = env.get_string(server).unwrap().into();
        let port: u16 = port as u16;
        let database: String = env.get_string(database).unwrap().into();
        let database: Database = database.parse().unwrap();
        let word: String = env.get_string(word).unwrap().into();

        let mut dict = Dict::connect(&server, port).unwrap();
        let cmd = Define::new(database, word);
        let defns: Vec<Definition> = dict.define(cmd).unwrap().collect();

        let java_definition_class = env.find_class("com/savanto/andict/Definition").unwrap();

        let definitions = env.new_object_array(
            defns.len() as i32,
            java_definition_class,
            JObject::null()
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
}
