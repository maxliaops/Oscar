#include "org_ungoverned_osgi_bundle_simple_SimpleBundle.h"

JNIEXPORT jstring JNICALL
    Java_org_ungoverned_osgi_bundle_simple_SimpleBundle_foo
        (JNIEnv *env, jobject obj)
{
    char *cstr = "Hello!";
    jstring jstr = (*env)->NewStringUTF(env, cstr);
    return jstr;
}
