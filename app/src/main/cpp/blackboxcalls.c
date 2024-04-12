#include <string.h>
#include <jni.h>
#include "blackbox.h"
#include <stdbool.h>
#include <android/log.h>

#define TAG "MY_TAG"
JavaVM *g_vm;


JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_ec_bond_activity_SplashScreenActivity_bb_1set_1home_1env_1dir(JNIEnv *env, jobject thiz, jstring path) {
    const char *path_value = (*env)->GetStringUTFChars(env, path, 0);
    int a = setenv("HOME", path_value, 1);
    (*env)->ReleaseStringUTFChars(env, path, path_value);
}

JNIEXPORT jbyteArray JNICALL
Java_com_ec_bond_model_SignUpViewModel_bb_1encrypt_1pwdconf(JNIEnv *env, jobject thiz, jstring pwdconf, jstring key, jint pwdconfenclen, jstring tmpfolder) {

    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    unsigned char pwdconfenc_value[pwdconfenclen];
    memset(pwdconfenc_value, 0x0, pwdconfenclen);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, false);
    char *key_value = (char *)(*env)->GetStringUTFChars(env, key, 0);
    char *tmpfolder_value = (char *)(*env)->GetStringUTFChars(env, tmpfolder, 0);

    jint output = bb_encrypt_pwdconf(pwdconf_value, key_value, pwdconfenc_value, tmpfolder_value);

    jbyteArray b=(*env)->NewByteArray(env, output);
    (*env)->SetByteArrayRegion(env, b, 0, output, (jbyte *)pwdconfenc_value);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, key, key_value);
    (*env)->ReleaseStringUTFChars(env, tmpfolder, tmpfolder_value);

    return b;
}


JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1decrypt_1pwdconf(JNIEnv *env, jobject thiz,
                                                                 jbyteArray pwdconfenc,
                                                                 jint pwdconfenclen, jstring keyp,
                                                                 jint pwdconflen,
                                                                 jstring tmpfolder) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

        char *key_value = (char *) (*env)->GetStringUTFChars(env, keyp, 0);
        char *tmpfolder_value = (char *) (*env)->GetStringUTFChars(env, tmpfolder, 0);
        jclass sys_cls =(*env)->FindClass(env,"com/ec/bond/utils/TestClass");

        jbyte *encryptedBytes = (*env)->GetByteArrayElements(env, pwdconfenc, 0);

        char pwd_conf[pwdconflen];
        memset(pwd_conf, 0x0, pwdconflen);

        char key[512] = {0};

        jint output = bb_decrypt_pwdconf((unsigned char *) encryptedBytes, pwdconfenclen, key_value,
                                         pwd_conf, tmpfolder_value);



    if (sys_cls != NULL){
        static jmethodID this_method_id = NULL;
        this_method_id =(*env)->GetStaticMethodID(env,sys_cls, "getAnswerC", "(Ljava/lang/String;)Ljava/lang/String;");
        jstring result1= (jstring)(*env)->CallStaticObjectMethod(env,sys_cls, this_method_id,(*env)->NewStringUTF(env,pwd_conf));
       // __android_log_print(ANDROID_LOG_DEBUG, TAG, result1 ,pwd_conf,pwd_conf);
       if(result1!=NULL){
           jstring result = (*env)->NewStringUTF(env, pwd_conf);
           (*env)->ReleaseByteArrayElements(env, pwdconfenc, encryptedBytes, 0);
           (*env)->ReleaseStringUTFChars(env, keyp, key_value);
           (*env)->ReleaseStringUTFChars(env, tmpfolder, tmpfolder_value);
           return result;

       }else{
           jstring result = (*env)->NewStringUTF(env, "error");
           (*env)->ReleaseByteArrayElements(env, pwdconfenc, encryptedBytes, 0);
           (*env)->ReleaseStringUTFChars(env, keyp, key_value);
           (*env)->ReleaseStringUTFChars(env, tmpfolder, tmpfolder_value);
           return result;

       }
    }else{
        jstring result = (*env)->NewStringUTF(env, "error");
        (*env)->ReleaseByteArrayElements(env, pwdconfenc, encryptedBytes, 0);
        (*env)->ReleaseStringUTFChars(env, keyp, key_value);
        (*env)->ReleaseStringUTFChars(env, tmpfolder, tmpfolder_value);

        return result;
    }

}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1register_1presence(JNIEnv *env, jobject thiz,
                                                                          jstring pwdconf,
                                                                          jstring os,
                                                                          jstring uniqueid) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *os_value = (char *)(*env)->GetStringUTFChars(env, os, 0);
    char *uniqueid_value = (char *)(*env)->GetStringUTFChars(env, uniqueid, 0);

    char *output =  bb_register_presence(pwdconf_value, os_value, uniqueid_value,uniqueid_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, os, os_value);
    (*env)->ReleaseStringUTFChars(env, uniqueid, uniqueid_value);
    free(output);


    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_model_SignUpViewModel_bb_1signup_1newdevice(JNIEnv *env, jobject thiz, jstring mobile, jstring otp, jstring smsotp) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *mobile_value = (char *)(*env)->GetStringUTFChars(env, mobile, false);
    char *otp_value = (char *)(*env)->GetStringUTFChars(env, otp, 0);
    char *smsotp_value = (char *)(*env)->GetStringUTFChars(env, smsotp, 0);

    char *output = bb_signup_newdevice(mobile_value, otp_value, smsotp_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, mobile, mobile_value);
    (*env)->ReleaseStringUTFChars(env, otp, otp_value);
    (*env)->ReleaseStringUTFChars(env, smsotp, smsotp_value);
    free(output);


    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1get_1profileinfo(JNIEnv *env, jobject thiz,
                                                                 jstring recipient,
                                                                 jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char * output = bb_get_profileinfo(recipient_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1get_1configuration(JNIEnv *env, jobject thiz,
                                                                          jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char*)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_get_configuration(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);


    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1get_1contacts(JNIEnv *env, jobject thiz,
                                                              jstring search_text, jint contactid,
                                                              jint flagsearch, jint limitsearch,
                                                              jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *search_text_value = (char *)(*env)->GetStringUTFChars(env, search_text, 0);
    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output =  bb_get_contacts(search_text_value, contactid, flagsearch, limitsearch, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, search_text, search_text_value);
    free(output);


    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1last_1voicecalls(JNIEnv *env, jobject thiz,
                                                                 jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_last_voicecalls(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}



JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1get_1registered_1mobilenumber(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_get_registered_mobilenumber(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1get_1list_1chat(JNIEnv *env, jobject thiz,
                                                                jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char* output = bb_get_list_chat(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1set_1configuration(JNIEnv *env, jobject thiz,
                                                                          jstring pwdconf,
                                                                          jstring calendar,
                                                                          jstring language,
                                                                          jstring onlinevisibility,
                                                                          jstring autodownloadphotos,
                                                                          jstring autodownloadaudio,
                                                                          jstring autodownloadvideos,
                                                                          jstring autodownloaddocuments) {

    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *calendar_value = (char *)(*env)->GetStringUTFChars(env, calendar, 0);
    char *language_value = (char *)(*env)->GetStringUTFChars(env, language, 0);
    char *onlinevisibility_value = (char *)(*env)->GetStringUTFChars(env, onlinevisibility, 0);
    char *autodownloadphotos_value = (char *)(*env)->GetStringUTFChars(env, autodownloadphotos, 0);
    char *autodownloadaudio_value = (char *)(*env)->GetStringUTFChars(env, autodownloadaudio, 0);
    char *autodownloadvideos_value = (char *)(*env)->GetStringUTFChars(env, autodownloadvideos, 0);
    char *autodownloaddocuments_value = (char *)(*env)->GetStringUTFChars(env, autodownloaddocuments, 0);

    char *output = bb_set_configuration(
            pwdconf_value,
            calendar_value,
            language_value,
            onlinevisibility_value,
            autodownloadphotos_value,
            autodownloadaudio_value,
            autodownloadvideos_value,
            autodownloaddocuments_value);

    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, calendar, calendar_value);
    (*env)->ReleaseStringUTFChars(env, language, language_value);
    (*env)->ReleaseStringUTFChars(env, onlinevisibility, onlinevisibility_value);
    (*env)->ReleaseStringUTFChars(env, autodownloadphotos, autodownloadphotos_value);
    (*env)->ReleaseStringUTFChars(env, autodownloadaudio, autodownloadaudio_value);
    (*env)->ReleaseStringUTFChars(env, autodownloadvideos, autodownloadvideos_value);
    (*env)->ReleaseStringUTFChars(env, autodownloaddocuments, autodownloaddocuments_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1send_1txt_1msg_1groupchat(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jstring groupid,
                                                                               jstring body,
                                                                               jstring replytomsgid,
                                                                               jstring replybody,
                                                                               jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *body_value = (char *)(*env)->GetStringUTFChars(env, body, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *replytomsgid_value = (char *)(*env)->GetStringUTFChars(env, replytomsgid, 0);
    char *replybody_value = (char *)(*env)->GetStringUTFChars(env, replybody, 0);

    char *output = bb_send_txt_msg_groupchat(groupid_value, body_value, pwdconf_value, replytomsgid_value, replybody_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, body, body_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, replytomsgid, replytomsgid_value);
    (*env)->ReleaseStringUTFChars(env, replybody, replybody_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1get_1msgs_1fileasync(JNIEnv *env, jobject thiz,
                                                                         jstring pwdconf,
                                                                         jstring recipient,
                                                                         jstring groupid,
                                                                         jstring from_id,
                                                                         jstring to_id,
                                                                         jstring from_date,
                                                                         jstring to_date,
                                                                         jint limit) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *from_id_value = (char *)(*env)->GetStringUTFChars(env, from_id, 0);
    char *to_id_value = (char *)(*env)->GetStringUTFChars(env, to_id, 0);
    char *from_date_value = (char *)(*env)->GetStringUTFChars(env, from_date, 0);
    char *to_date_value = (char *)(*env)->GetStringUTFChars(env, to_date, 0);

    char *output = bb_get_msgs_fileasync(pwdconf_value, recipient_value, from_id_value, to_id_value, from_date_value, to_date_value, groupid_value, limit);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, from_id, from_id_value);
    (*env)->ReleaseStringUTFChars(env, to_id, to_id_value);
    (*env)->ReleaseStringUTFChars(env, from_date, from_date_value);
    (*env)->ReleaseStringUTFChars(env, to_date, to_date_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1add_1contact(JNIEnv *env, jobject thiz,
                                                             jstring contact_json,
                                                             jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *contact_json_value = (char *)(*env)->GetStringUTFChars(env, contact_json, 0);

    char *output = bb_add_contact(contact_json_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, contact_json, contact_json_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1update_1contact(JNIEnv *env, jobject thiz,
                                                    jstring contact_json,
                                                    jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);


    char *contact_json_value = (char *)(*env)->GetStringUTFChars(env, contact_json, 0);

    char *output = bb_update_contact(contact_json_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, contact_json, contact_json_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1get_1list_1members_1groupchat(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jstring groupid,
                                                                                   jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_get_list_members_groupchat(groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    free(output);
    return result;
}


JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1set_1onoffline(JNIEnv *env, jobject thiz,
                                                                      jstring pwdconf,
                                                                      jstring status) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *status_value = (char *)(*env)->GetStringUTFChars(env, status, 0);

    char *output = bb_set_onoffline(pwdconf_value, status_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, status, status_value);
    free(output);
    return result;
}



JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1update_1photo_1profile(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring path,
                                                                              jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *path_value = (char *)(*env)->GetStringUTFChars(env, path, 0);

    char *output = bb_update_photo_profile(path_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, path, path_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBContact_bb_1get_1photoprofile_1filename(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jstring number,
                                                                                   jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *number_value = (char *)(*env)->GetStringUTFChars(env, number, 0);

    char *output = bb_get_photoprofile_filename(number_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, number, number_value);
    free(output);
    return result;
}

JNIEXPORT void JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1push_1messages_1client(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring number) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    jclass fieldClass = (*env)->FindClass(env, "com/ec/bond/blackbox/model/BBAccount");
    jmethodID methodId = (*env)->GetMethodID(env, fieldClass, "internalPushCallBack", "(I)V");

    char *number_value = (char *)(*env)->GetStringUTFChars(env, number, 0);
    bb_push_messages_client_android(number_value, env, thiz, methodId);

}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1get_1newmsg_1fileasync(JNIEnv *env, jobject thiz,
                                                                       jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_get_newmsg_fileasync(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1send_1file_1groupchat(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jstring filename,
                                                                           jstring groupid,
                                                                           jstring body,
                                                                           jstring replytomsgid,
                                                                           jstring replybody,
                                                                           jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *filename_value = (char *)(*env)->GetStringUTFChars(env, filename, 0);
    char *body_value = (char *)(*env)->GetStringUTFChars(env, body, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *replytomsgid_value = (char *)(*env)->GetStringUTFChars(env, replytomsgid, 0);
    char *replybody_value = (char *)(*env)->GetStringUTFChars(env, replybody, 0);

    char *output = bb_send_file_groupchat(filename_value, groupid_value, body_value, pwdconf_value, replytomsgid_value, replybody_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, filename, filename_value);
    (*env)->ReleaseStringUTFChars(env, body, body_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, replytomsgid, replytomsgid_value);
    (*env)->ReleaseStringUTFChars(env, replybody, replybody_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1send_1location_1groupchat(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jstring groupid,
                                                                               jstring latitude,
                                                                               jstring longitude,
                                                                               jstring replytomsgid,
                                                                               jstring replybody,
                                                                               jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *latitude_value = (char *)(*env)->GetStringUTFChars(env, latitude, 0);
    char *longitude_value = (char *)(*env)->GetStringUTFChars(env, longitude, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *replytomsgid_value = (char *)(*env)->GetStringUTFChars(env, replytomsgid, 0);
    char *replybody_value = (char *)(*env)->GetStringUTFChars(env, replybody, 0);

    char *output = bb_send_location_groupchat(groupid_value, latitude_value, longitude_value, pwdconf_value, replytomsgid_value, replybody_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, latitude, latitude_value);
    (*env)->ReleaseStringUTFChars(env, longitude, longitude_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, replytomsgid, replytomsgid_value);
    (*env)->ReleaseStringUTFChars(env, replybody, replybody_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1send_1typing_1groupchat(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jstring groupid,
                                                                             jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_send_typing_groupchat(groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1delete_1chat(JNIEnv *env, jobject thiz,
                                                                 jstring contactnumber,
                                                                 jstring groupid, jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *contactnumber_value = (char *)(*env)->GetStringUTFChars(env, contactnumber, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_delete_chat(pwdconf_value, contactnumber_value, groupid_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, contactnumber, contactnumber_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1update_1photo_1groupchat(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring filepath,
                                                                              jstring groupid,
                                                                              jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *filepath_value = (char *)(*env)->GetStringUTFChars(env, filepath, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_update_photo_groupchat(filepath_value, groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, filepath, filepath_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1get_1photo(JNIEnv *env, jobject thiz,
                                                               jstring filename, jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *filename_value = (char *)(*env)->GetStringUTFChars(env, filename, 0);

    char *output = bb_get_photo(filename_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, filename, filename_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBContact_bb_1send_1txt_1msg(JNIEnv *env, jobject thiz,
                                                                      jstring recipient,
                                                                      jstring body,
                                                                      jstring replytomsgid,
                                                                      jstring replybody,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *body_value = (char *)(*env)->GetStringUTFChars(env, body, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *replytomsgid_value = (char *)(*env)->GetStringUTFChars(env, replytomsgid, 0);
    char *replybody_value = (char *)(*env)->GetStringUTFChars(env, replybody, 0);

    char *output = bb_send_txt_msg(recipient_value, body_value, pwdconf_value, replytomsgid_value, replybody_value);
    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, body, body_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, replytomsgid, replytomsgid_value);
    (*env)->ReleaseStringUTFChars(env, replybody, replybody_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBContact_bb_1send_1file(JNIEnv *env, jobject thiz,
                                                                  jstring filepath,
                                                                  jstring recipient, jstring body,
                                                                  jstring replytomsgid,
                                                                  jstring replybody,
                                                                  jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *filepath_value = (char *)(*env)->GetStringUTFChars(env, filepath, 0);
    char *body_value = (char *)(*env)->GetStringUTFChars(env, body, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *replytomsgid_value = (char *)(*env)->GetStringUTFChars(env, replytomsgid, 0);
    char *replybody_value = (char *)(*env)->GetStringUTFChars(env, replybody, 0);

    char *output = bb_send_file(filepath_value, recipient_value, body_value, pwdconf_value, replytomsgid_value, replybody_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, filepath, filepath_value);
    (*env)->ReleaseStringUTFChars(env, body, body_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, replytomsgid, replytomsgid_value);
    (*env)->ReleaseStringUTFChars(env, replybody, replybody_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBContact_bb_1send_1location(JNIEnv *env, jobject thiz,
                                                                      jstring recipient,
                                                                      jstring latitude,
                                                                      jstring longitude,
                                                                      jstring replytomsgid,
                                                                      jstring replybody,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *latitude_value = (char *)(*env)->GetStringUTFChars(env, latitude, 0);
    char *longitude_value = (char *)(*env)->GetStringUTFChars(env, longitude, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *replytomsgid_value = (char *)(*env)->GetStringUTFChars(env, replytomsgid, 0);
    char *replybody_value = (char *)(*env)->GetStringUTFChars(env, replybody, 0);

    char *output = bb_send_location(recipient_value, latitude_value, longitude_value, pwdconf_value, replytomsgid_value, replybody_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, latitude, latitude_value);
    (*env)->ReleaseStringUTFChars(env, longitude, longitude_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, replytomsgid, replytomsgid_value);
    (*env)->ReleaseStringUTFChars(env, replybody, replybody_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBContact_bb_1send_1typing(JNIEnv *env, jobject thiz,
                                                                    jstring recipient,
                                                                    jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char *output = bb_send_typing(recipient_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;
}
JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1set_1starredmsg(JNIEnv *env, jobject thiz,
                                                                          jstring msgid,
                                                                          jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *msgid_value = (char*)(*env)->GetStringUTFChars(env, msgid, 0);

    char *pwdconf_value = (char*)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_set_starredmsg(msgid_value,pwdconf_value);



    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1filetransfer_1getstatus(JNIEnv *env, jobject thiz,
                                                                        jstring filename) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *filename_value = (char *)(*env)->GetStringUTFChars(env, filename, 0);

    jint output = bb_filetransfer_getstatus(filename_value);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, filename, filename_value);

    return output;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1audio_1receive(JNIEnv *env, jobject thiz, jbyteArray audioBuffer) {

}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1audio_1send_1session(JNIEnv *env, jobject thiz,
                                                                     jint session,
                                                                     jbyteArray audio_buffer) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    // Audio packets must be 1920 size
    int dataSize = 1920;
    if((*env)->GetArrayLength(env, audio_buffer) != dataSize)
    {
        return 0;
    }

    // Convert to primitive array
    void *buffer = (*env)->GetPrimitiveArrayCritical(env, (jarray)audio_buffer, 0);
    jint output = bb_audio_send_session(session, buffer);
    // Release the primitive array
    (*env)->ReleasePrimitiveArrayCritical(env, audio_buffer, buffer, 0);

    return output;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1originate_1voicecall_1id(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jint session,
                                                                             jstring recipient,
                                                                             jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char *output = bb_originate_voicecall_id(recipient_value, pwdconf_value, session);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1answer_1voicecall(JNIEnv *env, jobject thiz,
                                                                          jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_answer_voicecall(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1status_1voicecall_1id(JNIEnv *env, jobject thiz,
                                                                          jstring callid,
                                                                          jint session,
                                                                          jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *callid_value = (char *)(*env)->GetStringUTFChars(env, callid, 0);

    char *output = bb_status_voicecall_id(pwdconf_value, callid_value, session);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, callid, callid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1hangup_1voicecall_1id(JNIEnv *env, jobject thiz,
                                                                          jstring callid,
                                                                          jint session,
                                                                          jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *callid_value = (char *)(*env)->GetStringUTFChars(env, callid, 0);

    char *output = bb_hangup_voicecall_id(pwdconf_value, callid_value, session);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, callid, callid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1info_1voicecall(JNIEnv *env, jobject thiz,
                                                                    jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *output = bb_info_voicecall(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1unset_1starredmsg(JNIEnv *env, jobject thiz,
                                                                     jstring msgid,
                                                                     jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *msgid_value = (char *) (*env)->GetStringUTFChars(env, msgid, 0);

    char *pwdconf_value = (char *) (*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_unset_starredmsg(msgid_value, pwdconf_value);


    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1info_1videocall(JNIEnv *env, jobject thiz,
                                                                    jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *output = bb_info_videocall(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1answer_1videocall(JNIEnv *env, jobject thiz,
                                                                      jstring audio_only,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *audio_only_value = (char *)(*env)->GetStringUTFChars(env, audio_only, 0);

    char *output = bb_answer_videocall(pwdconf_value, audio_only_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, audio_only, audio_only_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1status_1voicecall(JNIEnv *env, jobject thiz,
                                                                      jstring callid,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *callid_value = (char *)(*env)->GetStringUTFChars(env, callid, 0);

    char *output = bb_status_voicecall(pwdconf_value, callid_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, callid, callid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1delete_1message(JNIEnv *env, jobject thiz,
                                                                       jstring msgid,
                                                                       jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char*)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *msgid_value = (char*)(*env)->GetStringUTFChars(env, msgid, 0);

    char *output = bb_delete_message(pwdconf_value, msgid_value);

    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);


    return result;
}
JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1get_1newmsg(JNIEnv *env, jobject thiz,
                                                                     jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *) (*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_get_newmsg(pwdconf_value);


    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1originate_1voicecall(JNIEnv *env, jobject thiz,
                                                                         jstring recipient,
                                                                         jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char *output = bb_originate_voicecall(recipient_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1hangup_1voicecall(JNIEnv *env, jobject thiz,
                                                                      jstring callid,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *callid_value = (char *)(*env)->GetStringUTFChars(env, callid, 0);

    char *output = bb_hangup_voicecall(pwdconf_value, callid_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, callid, callid_value);
    free(output);

    return result;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1video_1send(JNIEnv *env, jobject thiz,
                                                                jbyteArray frame_buffer,
                                                                jint buffer_size) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    // Audio packets must be 1920 size
    if((*env)->GetArrayLength(env, frame_buffer) != buffer_size)
    {
        return 0;
    }

    // Convert to primitive array
    void *buffer = (*env)->GetPrimitiveArrayCritical(env, (jarray)frame_buffer, 0);
    jint output = bb_video_send(buffer, buffer_size);

    // Release the primitive array
    (*env)->ReleasePrimitiveArrayCritical(env, frame_buffer, buffer, 0);

    return output;
}

JNIEXPORT jbyteArray JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1video_1receive(JNIEnv *env, jobject thiz) {

    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    // Get the buffer and set the buffer size
    int bufferSize = 0;
    unsigned char *buffer = bb_video_receive(&bufferSize);

    if (bufferSize > 0) {

        // Create the java array
        jbyteArray frameBuffer = (*env)->NewByteArray(env, bufferSize);

        // Get a pointer to the primitive array and copy the buffer into it
        // We must treat the code inside this pair of functions as running in a "critical region."
        // Inside a critical region, native code must not call other JNI functions, or any system call
        // that may cause the current thread to block and wait for another Java thread. (For example,
        // the current thread must not call read on a stream being written by another Java thread.)
        void *temp = (*env)->GetPrimitiveArrayCritical(env, (jarray)frameBuffer, 0);
        memcpy(temp, buffer, bufferSize);
        // release temp and critical region
        (*env)->ReleasePrimitiveArrayCritical(env, frameBuffer, temp, 0);

        // free the buffer
        free(buffer);
        return frameBuffer;
    }
    return NULL;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1audio_1receive(JNIEnv *env, jobject thiz,
                                                                   jbyteArray audio_buffer) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    int dataSize = 1920;
    unsigned char *buffer = malloc(dataSize);
    jint output = bb_audio_receive(buffer);

    if((*env)->GetArrayLength(env, audio_buffer) != dataSize)
    {
        (*env)->DeleteLocalRef(env, audio_buffer);
        audio_buffer = (*env)->NewByteArray(env, dataSize);
    }

    void *temp = (*env)->GetPrimitiveArrayCritical(env, (jarray)audio_buffer, 0);
    memcpy(temp, buffer, dataSize);
    (*env)->ReleasePrimitiveArrayCritical(env, audio_buffer, temp, 0);

    return output;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1audio_1send(JNIEnv *env, jobject thiz,
                                                                jbyteArray audio_buffer) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    // Audio packets must be 1920 size
    int dataSize = 1920;
    if((*env)->GetArrayLength(env, audio_buffer) != dataSize)
    {
        return 0;
    }

    // Convert to primitive array
    void *buffer = (*env)->GetPrimitiveArrayCritical(env, (jarray)audio_buffer, 0);
    jint output = bb_audio_send(buffer);
    // Release the primitive array
    (*env)->ReleasePrimitiveArrayCritical(env, audio_buffer, buffer, 0);

    return output;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1audio_1send_1session(JNIEnv *env, jobject thiz,
                                                                         jint session,
                                                                         jbyteArray audio_buffer) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    // Audio packets must be 1920 size
    int dataSize = 1920;
    if((*env)->GetArrayLength(env, audio_buffer) != dataSize)
    {
        return 0;
    }

    // Convert to primitive array
    void *buffer = (*env)->GetPrimitiveArrayCritical(env, (jarray)audio_buffer, 0);
    jint output = bb_audio_send_session(session, buffer);
    // Release the primitive array
    (*env)->ReleasePrimitiveArrayCritical(env, audio_buffer, buffer, 0);

    return output;
}

JNIEXPORT jint JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1audio_1receive_1session(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jint session,
                                                                            jbyteArray audio_buffer) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    int dataSize = 1920;
    unsigned char *buffer = malloc(dataSize);
    jint output = bb_audio_receive_session(session, buffer);

    if((*env)->GetArrayLength(env, audio_buffer) != dataSize)
    {
        (*env)->DeleteLocalRef(env, audio_buffer);
        audio_buffer = (*env)->NewByteArray(env, dataSize);
    }

    void *temp = (*env)->GetPrimitiveArrayCritical(env, (jarray)audio_buffer, 0);
    memcpy(temp, buffer, dataSize);
    (*env)->ReleasePrimitiveArrayCritical(env, audio_buffer, temp, 0);

    return output;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1new_1groupchat(JNIEnv *env, jobject thiz,
                                                               jstring description,
                                                               jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *description_value = (char *)(*env)->GetStringUTFChars(env, description, 0);

    char *output = bb_new_groupchat(description_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, description, description_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1add_1member_1groupchat(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jstring groupid,
                                                                            jstring number,
                                                                            jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *number_value = (char *)(*env)->GetStringUTFChars(env, number, 0);

    char *output = bb_add_member_groupchat(groupid_value, number_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, number, number_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1delete_1groupchat(JNIEnv *env, jobject thiz,
                                                                       jstring groupid,
                                                                       jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_delete_groupchat(groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1change_1groupchat(JNIEnv *env, jobject thiz,
                                                                       jstring groupid,
                                                                       jstring description,
                                                                       jstring pwdconf) {
//    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char* pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char* description_value = (char *)(*env)->GetStringUTFChars(env, description, 0);
    char* groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char* output = bb_change_groupchat(description_value, groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, description, description_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1send_1read_1receipt(JNIEnv *env, jobject thiz,
                                                                         jstring recipient,
                                                                         jint msgid,
                                                                jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char* recipient_value = (char *) (*env)->GetStringUTFChars(env, recipient, 0);
    char* pwdconf_value = (char *) (*env)->GetStringUTFChars(env, pwdconf, 0);

    char* output = bb_send_read_receipt(recipient_value, msgid, pwdconf_value);


    jstring result = (*env)->NewStringUTF(env, output);

    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1revoke_1member_1groupchat(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jstring groupid,
                                                                               jstring number,
                                                                               jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *number_value = (char *)(*env)->GetStringUTFChars(env, number, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_revoke_member_groupchat(groupid_value, number_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, number, number_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1change_1role_1member_1groupchat(JNIEnv *env,
                                                                                     jobject thiz,
                                                                                     jstring groupid,
                                                                                     jstring number,
                                                                                     jstring role,
                                                                                     jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *number_value = (char *)(*env)->GetStringUTFChars(env, number, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *role_value = (char *)(*env)->GetStringUTFChars(env, role, 0);

    char *output = bb_change_role_member_groupchat(groupid_value, number_value, role_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, number, number_value);
    (*env)->ReleaseStringUTFChars(env, role, role_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBGroup_bb_1setexpiringdate_1groupchat(JNIEnv *env,
                                                                                jobject thiz,
                                                                                jstring groupid,
                                                                                jstring date,
                                                                                jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *date_value = (char *)(*env)->GetStringUTFChars(env, date, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);


    char *output = bb_setexpiringdate_groupchat(date_value, groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, date, date_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1set_1forwardedmsg(JNIEnv *env, jobject thiz,
                                                                         jstring msgid,
                                                                         jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *msgid_value = (char *)(*env)->GetStringUTFChars(env, msgid, 0);

    char *output = bb_set_forwardedmsg(msgid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, msgid, msgid_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1originate_1videocall(JNIEnv *env, jobject thiz,
                                                                         jstring recipient,
                                                                         jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char *output = bb_originate_videocall(recipient_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1status_1videocall(JNIEnv *env, jobject thiz,
                                                                      jstring call_id,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *call_id_value = (char *)(*env)->GetStringUTFChars(env, call_id, 0);

    char *output = bb_status_videocall(pwdconf_value, call_id_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, call_id, call_id_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1hangup_1videocall(JNIEnv *env, jobject thiz,
                                                                      jstring call_id,
                                                                      jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *call_id_value = (char *)(*env)->GetStringUTFChars(env, call_id, 0);

    char *output = bb_hangup_videocall(pwdconf_value, call_id_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, call_id, call_id_value);
    free(output);

    return result;
}


JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1confirm_1videocall(JNIEnv *env, jobject thiz,
                                                                     jstring callid,
                                                                     jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *callid_value = (char *)(*env)->GetStringUTFChars(env, callid, 0);

    char *output = bb_confirm_videocall(pwdconf_value, callid_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, callid, callid_value);

    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBContact_bb_1get_1profileinfo(JNIEnv *env, jobject thiz,
                                                                        jstring number,
                                                                        jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *number_value = (char *)(*env)->GetStringUTFChars(env, number, 0);

    char *output = bb_get_profileinfo(number_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, number, number_value);

    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1autodelete_1chat(JNIEnv *env, jobject thiz,
                                                                     jint seconds,
                                                                     jstring recipient,
                                                                     jstring groupid,
                                                                     jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_autodelete_chat(pwdconf_value, recipient_value, groupid_value, seconds);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);

    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1set_1archivedchat(JNIEnv *env, jobject thiz,
                                                                  jstring recipient,
                                                                  jstring groupid,
                                                                  jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_set_archivedchat(recipient_value, groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);

    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1unset_1archivedchat(JNIEnv *env, jobject thiz,
                                                                  jstring recipient,
                                                                  jstring groupid,
                                                                  jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_unset_archivedchat(recipient_value, groupid_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);

    free(output);

    return result;
}

JNIEXPORT void JNICALL
Java_com_ec_bond_blackbox_model_BBCall_bb_1audio_1set_1audioconference(JNIEnv *env,
                                                                                jobject thiz,
                                                                                jint session) {
    bb_audio_set_audioconference(session);
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1autodelete_1chat_1getconf(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring recipient,
                                                                              jstring groupid,
                                                                              jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);

    char *output = bb_autodelete_chat_getconf(pwdconf_value, recipient_value, groupid_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);

    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1delete_1contact(JNIEnv *env, jobject thiz, jstring contact_id, jstring pwdconf) {
    (*g_vm)->AttachCurrentThread(g_vm, &env, NULL);

    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *contact_id_value = (char *)(*env)->GetStringUTFChars(env, contact_id, 0);

    char *output = bb_delete_contact(contact_id_value, pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, contact_id, contact_id_value);

    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1set_1notification(JNIEnv *env, jobject thiz,
                                                                  jstring recipient,
                                                                  jstring groupid,
                                                                  jstring sound_name,
                                                                  jstring pwdconf) {
    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *sound_name_value = (char *)(*env)->GetStringUTFChars(env, sound_name, 0);
    char *vibration = "Y";
    char *priority = "Y";
    char *popup = "Y";
    char *dtmute = "0000-00-00 00:00:00";

    char *output = bb_set_notifications(pwdconf_value, groupid_value, recipient_value, sound_name_value, vibration, priority, popup, dtmute);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, sound_name, sound_name_value);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1get_1notifications(JNIEnv *env, jobject thiz,
                                                                   jstring pwdconf) {
    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);

    char *output = bb_get_notifications(pwdconf_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    free(output);

    return result;
}

JNIEXPORT void JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1register_1internal_1push(JNIEnv *env,
                                                                                jobject thiz,
                                                                                jstring registered_number) {
    jclass fieldClass = (*env)->FindClass(env, "com/ec/bond/blackbox/model/BBAccount");
    jmethodID methodId = (*env)->GetMethodID(env, fieldClass, "internalPushCallBack", "(I)V");

    char *number_value = (char *)(*env)->GetStringUTFChars(env, registered_number, 0);
    bb_push_messages_client_android(number_value, env, thiz, methodId);
    (*env)->ReleaseStringUTFChars(env, registered_number, number_value);
}

JNIEXPORT void JNICALL
Java_com_ec_bond_blackbox_model_BBAccount_bb_1unregister_1internal_1push(JNIEnv *env,
                                                                                  jobject thiz) {
    bb_push_messages_client_close();
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_model_BBChat_bb_1get_1starred_1messages(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jstring recipient,
                                                                           jstring groupid,
                                                                           jstring pwdconf) {
    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char *output = bb_get_starredmsg(pwdconf_value, groupid_value, recipient_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_ec_bond_blackbox_Blackbox_bb_1get_1starred_1messages(JNIEnv *env, jobject thiz,
                                                              jstring recipient, jstring groupid,
                                                              jstring pwdconf) {
    char *pwdconf_value = (char *)(*env)->GetStringUTFChars(env, pwdconf, 0);
    char *groupid_value = (char *)(*env)->GetStringUTFChars(env, groupid, 0);
    char *recipient_value = (char *)(*env)->GetStringUTFChars(env, recipient, 0);

    char *output = bb_get_starredmsg(pwdconf_value, groupid_value, recipient_value);
    jstring result = (*env)->NewStringUTF(env, output);

    // Release memory
    (*env)->ReleaseStringUTFChars(env, pwdconf, pwdconf_value);
    (*env)->ReleaseStringUTFChars(env, groupid, groupid_value);
    (*env)->ReleaseStringUTFChars(env, recipient, recipient_value);
    free(output);

    return result;

}