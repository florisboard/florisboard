use dummy;
use nlp::NlpEngine;
use std::sync::LazyLock;

use jni::objects::{JByteArray, JClass, JObjectArray, JString};
use jni::sys::{jboolean, jdouble, jint, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

static ENGINE: LazyLock<NlpEngine> = LazyLock::new(NlpEngine::new);

fn read_context(env: &mut JNIEnv, context_json: &JString) -> Vec<String> {
    match env.get_string(context_json) {
        Ok(s) => {
            let json: String = s.into();
            serde_json::from_str(&json).unwrap_or_default()
        }
        Err(_) => vec![],
    }
}

fn to_jstring(env: &JNIEnv, value: String) -> jstring {
    env.new_string(value)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_TestKt_dummyAdd(
    _env: JNIEnv,
    _class: JClass,
    a: jint,
    b: jint,
) -> jint {
    dummy::addnumbers(a, b)
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeSpellCheck(
    mut env: JNIEnv,
    _class: JClass,
    word: JString,
    context_json: JString,
    max_suggestions: jint,
) -> jstring {
    let word_str: String = match env.get_string(&word) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let context = read_context(&mut env, &context_json);

    let result = ENGINE.spell_check(&word_str, &context, max_suggestions as usize);
    to_jstring(&env, serde_json::to_string(&result).unwrap_or_default())
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeSuggest(
    mut env: JNIEnv,
    _class: JClass,
    prefix: JString,
    context_json: JString,
    max_count: jint,
) -> jstring {
    let prefix_str: String = match env.get_string(&prefix) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let context = read_context(&mut env, &context_json);

    let suggestions = ENGINE.suggest(&prefix_str, &context, max_count as usize);
    to_jstring(&env, serde_json::to_string(&suggestions).unwrap_or_default())
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeLearnWord(
    mut env: JNIEnv,
    _class: JClass,
    word: JString,
    context_json: JString,
) {
    let word_str: String = match env.get_string(&word) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let context = read_context(&mut env, &context_json);
    ENGINE.learn_word(&word_str, &context);
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativePenalizeWord(
    mut env: JNIEnv,
    _class: JClass,
    word: JString,
) {
    if let Ok(s) = env.get_string(&word) {
        let word_str: String = s.into();
        ENGINE.penalize_word(&word_str);
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeRemoveWord(
    mut env: JNIEnv,
    _class: JClass,
    word: JString,
) -> jboolean {
    match env.get_string(&word) {
        Ok(s) => {
            let word_str: String = s.into();
            if ENGINE.remove_word(&word_str) {
                JNI_TRUE
            } else {
                JNI_FALSE
            }
        }
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeGetFrequency(
    mut env: JNIEnv,
    _class: JClass,
    word: JString,
) -> jdouble {
    match env.get_string(&word) {
        Ok(s) => {
            let word_str: String = s.into();
            ENGINE.get_frequency(&word_str)
        }
        Err(_) => 0.0,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeExportPersonalDict(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    to_jstring(&env, ENGINE.export_personal_dict())
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeImportPersonalDict(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jboolean {
    let json: String = match env.get_string(&json_data) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    match ENGINE.import_personal_dict(&json) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeSetLanguages(
    mut env: JNIEnv,
    _class: JClass,
    languages: JObjectArray,
) {
    let len = match env.get_array_length(&languages) {
        Ok(l) => l as usize,
        Err(_) => return,
    };
    let mut lang_vec: Vec<String> = Vec::with_capacity(len);
    for i in 0..len {
        let obj = match env.get_object_array_element(&languages, i as i32) {
            Ok(o) => o,
            Err(_) => continue,
        };
        let jstr: JString = obj.into();
        let java_str = match env.get_string(&jstr) {
            Ok(s) => s,
            Err(_) => continue,
        };
        lang_vec.push(java_str.into());
    }
    let refs: Vec<&str> = lang_vec.iter().map(|s| s.as_str()).collect();
    ENGINE.set_languages(&refs);
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeLoadDictionaryBinaryForLanguage(
    mut env: JNIEnv,
    _class: JClass,
    language: JString,
    data: JByteArray,
) -> jboolean {
    let lang: String = match env.get_string(&language) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let bytes = match env.convert_byte_array(&data) {
        Ok(b) => b,
        Err(_) => return JNI_FALSE,
    };
    match ENGINE.load_dictionary_binary_for_language(&lang, &bytes) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeExportContextMap(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    to_jstring(&env, ENGINE.export_context_map())
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeImportContextMap(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jboolean {
    let json: String = match env.get_string(&json_data) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    match ENGINE.import_context_map(&json) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeClear(
    _env: JNIEnv,
    _class: JClass,
) {
    ENGINE.clear();
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeLoadNgramsForLanguage(
    mut env: JNIEnv,
    _class: JClass,
    language: JString,
    json_data: JString,
) -> jboolean {
    let lang: String = match env.get_string(&language) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    let json: String = match env.get_string(&json_data) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    match ENGINE.load_ngrams_for_language(&lang, &json) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativePredictNextWord(
    mut env: JNIEnv,
    _class: JClass,
    context_json: JString,
    max_count: jint,
) -> jstring {
    let context = read_context(&mut env, &context_json);

    let predictions = ENGINE.predict_next_word(&context, max_count as usize);
    to_jstring(&env, serde_json::to_string(&predictions).unwrap_or_default())
}
