use dummy;

use jni::objects::JClass;
use jni::sys::jint;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_org_florisboard_libnative_TestKt_dummyAdd(
    _env: JNIEnv,
    _class: JClass,
    a: jint,
    b: jint,
) -> jint {
    dummy::addnumbers(a, b)
}
