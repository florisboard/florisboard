use dummy;
use nlp::NlpEngine;
use once_cell::sync::Lazy;

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jdouble, jint, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

static ENGINE: Lazy<NlpEngine> = Lazy::new(NlpEngine::new);

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
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeLoadDictionary(
    mut env: JNIEnv,
    _class: JClass,
    json_data: JString,
) -> jboolean {
    let json: String = match env.get_string(&json_data) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE,
    };
    match ENGINE.load_dictionary(&json) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeLoadDictionaryBinary(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jboolean {
    let bytes = match env.convert_byte_array(&data) {
        Ok(b) => b,
        Err(_) => return JNI_FALSE,
    };
    match ENGINE.load_dictionary_binary(&bytes) {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
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
    let context: Vec<String> = match env.get_string(&context_json) {
        Ok(s) => {
            let json: String = s.into();
            serde_json::from_str(&json).unwrap_or_default()
        }
        Err(_) => vec![],
    };

    let result = ENGINE.spell_check(&word_str, &context, max_suggestions as usize);
    let json = serde_json::to_string(&result).unwrap_or_default();

    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
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
    let context: Vec<String> = match env.get_string(&context_json) {
        Ok(s) => {
            let json: String = s.into();
            serde_json::from_str(&json).unwrap_or_default()
        }
        Err(_) => vec![],
    };

    let suggestions = ENGINE.suggest(&prefix_str, &context, max_count as usize);
    let json = serde_json::to_string(&suggestions).unwrap_or_default();

    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
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
    let context: Vec<String> = match env.get_string(&context_json) {
        Ok(s) => {
            let json: String = s.into();
            serde_json::from_str(&json).unwrap_or_default()
        }
        Err(_) => vec![],
    };
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
    let json = ENGINE.export_personal_dict();
    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
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
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeSetLanguage(
    mut env: JNIEnv,
    _class: JClass,
    language: JString,
) {
    if let Ok(s) = env.get_string(&language) {
        let lang: String = s.into();
        ENGINE.set_language(&lang);
    }
}

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_NlpBridgeKt_nativeGetLanguage(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let lang = ENGINE.get_language();
    env.new_string(lang)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
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
    let json = ENGINE.export_context_map();
    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
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
