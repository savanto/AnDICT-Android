extern crate jni;
extern crate dictp;

#[cfg(target_os="android")]
#[allow(non_snake_case)]
pub mod android {
    use jni::JNIEnv;
    use jni::objects::{JClass, JObject, JString};
    use jni::sys::{jint, jobjectArray};

    use dictp::{Database, Definition, Dict};

    #[no_mangle]
    pub extern fn Java_com_savanto_andict_NativeDict_define<'a>(
        env: JNIEnv,
        _class: JClass,
        server: JString,
        port: jint,
        database: JString,
        word: JString,
    ) -> jobjectArray {
        let server: String = env.get_string(server).unwrap().into();
        let port: u32 = port as u32;
        let database: String = env.get_string(database).unwrap().into();
        let database: Database = match database.as_str() {
            "*" => Database::AllMatches,
            "!" => Database::FirstMatch,
            _ => Database::Source(database),
        };
        let word: String = env.get_string(word).unwrap().into();

        let java_string_class = env.find_class("java/lang/String").unwrap();
        match Dict::define(&server, port, &word, &database) {
            Ok(dict) => {
                let entries: Vec<Definition> = dict.collect();
                let entries_array = env.new_object_array(
                    (2 * entries.len()) as i32,
                    java_string_class,
                    JObject::null()
                ).unwrap();

                let mut index: i32 = 0;
                for entry in entries {
                    env.set_object_array_element(
                        entries_array,
                        index,
                        JObject::from(env.new_string(entry.source).unwrap())
                    ).unwrap();
                    index += 1;

                    env.set_object_array_element(
                        entries_array,
                        index,
                        JObject::from(env.new_string(entry.definition).unwrap())
                    ).unwrap();
                    index += 1;
                }

                entries_array
            },
            Err(_) => {
                env.new_object_array(0, java_string_class, JObject::null()).unwrap()
            },
        }
    }
}
