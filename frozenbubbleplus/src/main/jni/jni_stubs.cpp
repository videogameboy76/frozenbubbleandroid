/*
 * Quick-n-dirty JNI interface for using libmodplug in
 * Android (P.A. Casey  www.peculiar-games.com)
 *
 * go ahead and use it however you like, although
 * attribution is always appreciated!
 */

/*
 * JNI and your interface class headers.
 */
#include <jni.h>
#include <android/log.h>

#include "stdafx.h"
#include "modplug.h"
#include "sndfile.h"

#include "andmodplug_PlayerThread.h"

struct _ModPlugFile
{
  CSoundFile mSoundFile;
};

const ModPlug_Settings gSettings8000 =
{
  MODPLUG_ENABLE_OVERSAMPLING    |
  MODPLUG_ENABLE_NOISE_REDUCTION |
  MODPLUG_ENABLE_REVERB,

  2,
  16,
  8000,
  MODPLUG_RESAMPLE_LINEAR,
  128,
  32,
  50,
  100,
  0,
  0,
  0
};

const ModPlug_Settings gSettings16000 =
{
  MODPLUG_ENABLE_OVERSAMPLING    |
  MODPLUG_ENABLE_NOISE_REDUCTION |
  MODPLUG_ENABLE_REVERB,

  2,
  16,
  16000,
  MODPLUG_RESAMPLE_LINEAR,
  128,
  32,
  50,
  100,
  0,
  0,
  0
};

const ModPlug_Settings gSettings22000 =
{
  MODPLUG_ENABLE_OVERSAMPLING    |
  MODPLUG_ENABLE_NOISE_REDUCTION |
  MODPLUG_ENABLE_REVERB,

  2,
  16,
  22000,
  MODPLUG_RESAMPLE_LINEAR,
  128,
  32,
  50,
  100,
  0,
  0,
  0
};

const ModPlug_Settings gSettings32000 =
{
  MODPLUG_ENABLE_OVERSAMPLING    |
  MODPLUG_ENABLE_NOISE_REDUCTION |
  MODPLUG_ENABLE_REVERB,

  2,
  16,
  32000,
  MODPLUG_RESAMPLE_LINEAR,
  128,
  32,
  50,
  100,
  0,
  0,
  0
};

//
// ADD FOLLOWING JNI INTERFACE FUNCTIONS after the header files
//
// ************************************************************
// Start of JNI stub code
// ************************************************************
ModPlugFile* currmodFile;

#define SAMPLEBUFFERSIZE 40000

unsigned char samplebuffer[SAMPLEBUFFERSIZE];

int currsample;

/*
 * DIAB hack to change tempo!!
 */
int DIABtempochange;
int DIABtempooverride;
int DIABholdpattern;
int DIABnextpattern;
int DIABpatternchanged;
int DIABoverridejump;
int DIABforcedpatternchange;

/*
 * More generalized pattern jumping...
 */
int ANDMODPLUGpatternfrom;
int ANDMODPLUGpatternto;
int ANDMODPLUGpendingfrom;
int ANDMODPLUGpendingto;
int ANDMODPLUGpatternrangeset;
int ANDMODPLUGnextpattern;
int ANDMODPLUGnextpatternmode;
int ANDMODPLUGjumpeffect;
int ANDMODPLUGlogoutput;

/*
 * Class:     com_peculiar_1games_andmodplug_PlayerThread
 * Method:    ModPlug_Init
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1Init
  (JNIEnv *env, jclass cls, jint rate)
{
  /*
   * If trying to make this truly re-entrant, separate buffers could be
   * allocated here.
   */
  __android_log_print(ANDROID_LOG_INFO, "JNI_STUBS", "Initializing modplug with rate %d", rate);

  switch (rate)
  {
    case 8000:
      ModPlug_SetSettings(&gSettings8000);
      break;
    case 16000:
      ModPlug_SetSettings(&gSettings16000);
      break;
    case 22000:
      ModPlug_SetSettings(&gSettings22000);
      break;
    case 32000:
      ModPlug_SetSettings(&gSettings32000);
      break;
    case 44100:
      /*
       * This is the default, so settings needn't be changed.
       */
      break;
  }

  currmodFile = NULL;
  DIABtempochange = 0;
  DIABtempooverride = 0;
  DIABpatternchanged = 0;
  ANDMODPLUGpatternrangeset = 0;
  ANDMODPLUGnextpattern = -1;
  ANDMODPLUGnextpatternmode = 0;
  ANDMODPLUGjumpeffect = -1;
  ANDMODPLUGlogoutput = 0;

  return JNI_TRUE;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_JLoad
 * Signature: ([BI)Z
 */
JNIEXPORT jboolean JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1JLoad
  (JNIEnv *env, jobject obj, jbyteArray buffer, jint size)
{
  int csize = (int) size;

  /*
   * Set the current sample as already beyond end of buffer (so a reload
   * happens immediately).
   */
  currsample = SAMPLEBUFFERSIZE+1;

  /*
   * Convert from Java buffer into a C buffer.
   */
  jbyte* bytes = env->GetByteArrayElements(buffer, 0);
  currmodFile = ModPlug_Load(bytes, csize);
  env->ReleaseByteArrayElements(buffer, bytes, 0);

  DIABpatternchanged = 0;
  ANDMODPLUGpatternfrom = 0;
  ANDMODPLUGpatternto = 0;
  ANDMODPLUGpendingfrom = 0;
  ANDMODPLUGpendingto = 0;
  ANDMODPLUGpatternrangeset = 0;
  ANDMODPLUGnextpattern = -1;
  ANDMODPLUGnextpatternmode = 0;
  ANDMODPLUGjumpeffect = -1;

  if (currmodFile != NULL)
  {
    return JNI_TRUE;
  }
  else
  {
    return JNI_FALSE;
  }
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_JGetName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1JGetName
  (JNIEnv *env, jobject obj)
{
  if (currmodFile != NULL)
    return env->NewStringUTF(ModPlug_GetName(currmodFile));
  else
    return NULL;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_JNumChannels
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1JNumChannels
  (JNIEnv *env, jobject obj)
{
  jint numchannels = 0;

  if (currmodFile != NULL)
    numchannels = ModPlug_NumChannels(currmodFile);
  return numchannels;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_GetCurrentPos
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1GetCurrentPos
  (JNIEnv *, jobject)
{
  jint curr = 0;

  if (currmodFile != NULL)
    curr = currmodFile->mSoundFile.GetCurrentPos();
  return curr;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_GetMaxPos
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1GetMaxPos
  (JNIEnv *, jobject)
{
  jint maxpos = 0;

  if (currmodFile != NULL)
    maxpos = currmodFile->mSoundFile.GetMaxPosition();
  return maxpos;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_GetCurrentOrder
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1GetCurrentOrder
  (JNIEnv *env, jobject obj)
{
  jint curr = 0;

  if (currmodFile != NULL)
    curr = ModPlug_GetCurrentOrder(currmodFile);
  return curr;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_GetCurrentPattern
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1GetCurrentPattern
  (JNIEnv *env, jobject obj)
{
  jint curr = 0;

  if (currmodFile != NULL)
    curr = ModPlug_GetCurrentPattern(currmodFile);
  return curr;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_GetCurrentRow
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1GetCurrentRow
  (JNIEnv *env, jobject obj)
{
  jint curr = 0;

  if (currmodFile != NULL)
    curr = ModPlug_GetCurrentRow(currmodFile);
  return curr;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_JGetSoundData
 * Signature: ([SI)I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1JGetSoundData
  (JNIEnv *env, jobject obj, jshortArray jbuffer, jint size)
{
  jint smpsize = 0;

  if (currmodFile == NULL)
    return 0;

#ifndef SMALLER_READS
  if (currsample >= SAMPLEBUFFERSIZE)
  {
    /*
     * Need to read another buffer full of sample data.
     */
    smpsize = ModPlug_Read(currmodFile, samplebuffer, SAMPLEBUFFERSIZE);
    if (smpsize)
    {
      currsample = 0;
    }
  }
#else // SMALLER_READS
  /*
   * In this mode, we read in exactly how much Java requested to improve
   * frame rate.
   */
  smpsize = ModPlug_Read(currmodFile, samplebuffer, size*sizeof(jshort));
  currsample = 0;
#endif // SMALLER_READS

  /*
   * Now convert the C sample buffer data to a java short array.
   */
  if (size && samplebuffer && (smpsize || currsample < SAMPLEBUFFERSIZE))
  {
    env->SetShortArrayRegion(jbuffer, 0 ,size, (jshort *) (((char *) samplebuffer)+currsample));
    currsample += size*sizeof(jshort);

    return size;
  }
  else
  {
    return 0;
  }
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_JUnload
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1JUnload
  (JNIEnv *env, jclass cls)
{
  if (currmodFile != NULL)
  {
    ModPlug_Unload(currmodFile);
    currmodFile = NULL;
  }

  return JNI_TRUE;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_CloseDown
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1CloseDown
  (JNIEnv *env, jclass cls)
{
  /*
   * Maybe for a proper re-entrant library, need to handle shutdown
   * stuff, deallocting buffers etc.,  but for my crappy, hacky single-
   * entry version, do nothing much...
   */

  return JNI_TRUE;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_GetNativeTempo
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1GetNativeTempo
  (JNIEnv *env, jclass cls)
{
  if (currmodFile != NULL)
    return currmodFile->mSoundFile.m_nMusicTempo;
  else
    return 0;
}

/*
 * Class:     com_peculiargames_modplayer_PlayerThread
 * Method:    ModPlug_ChangeTempo
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1ChangeTempo
  (JNIEnv *env, jclass cls, jint tc)
{
  /*
   * Hack the tempo.
   */
  DIABtempochange = tc;
}

/*
 * Class:     com_peculiargames_modplayer_PlayerThread
 * Method:    ModPlug_SetTempo
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1SetTempo
  (JNIEnv *env, jclass cls, jint to)
{
  /*
   * Hack the tempo.
   */
  DIABtempooverride = to;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_ChangePattern
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1ChangePattern
  (JNIEnv *env, jclass cls, jint newpattern)
{
  DIABnextpattern = newpattern-1;
  DIABpatternchanged = 0;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_RepeatPattern
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1RepeatPattern
  (JNIEnv *env, jclass cls, jint pattern)
{
  DIABholdpattern = 1;
  DIABnextpattern = pattern-1;
  DIABpatternchanged = 0;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_CheckPatternChange
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1CheckPatternChange
  (JNIEnv *env, jclass cls)
{
  if (DIABpatternchanged)
  {
    DIABpatternchanged = 0;
    return JNI_TRUE;
  }
  else
    return JNI_FALSE;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_SetPatternLoopMode
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1SetPatternLoopMode
  (JNIEnv *env, jclass cls, jboolean flag)
{
  if (flag == JNI_TRUE)
    DIABholdpattern = 1;
  else
    DIABholdpattern = 0;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_SetPatternLoopRange
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1SetPatternLoopRange
  (JNIEnv *env, jclass cls, jint from, jint to, jint when)
{
  if (ANDMODPLUGpatternrangeset == 0)
  {
    ANDMODPLUGpatternrangeset = 1;
    ANDMODPLUGpatternfrom = from;
    ANDMODPLUGpatternto = to;
    __android_log_print(ANDROID_LOG_INFO, "JNI_STUBS", "ANDMODPLUGpatternfrom=%d to=%d",
        ANDMODPLUGpatternfrom, ANDMODPLUGpatternto);
  }
  else
  {
    ANDMODPLUGpendingfrom = from;
    ANDMODPLUGpendingto = to;
    __android_log_print(ANDROID_LOG_INFO, "JNI_STUBS", "ANDMODPLUGpendingfrom=%d to=%d",
        ANDMODPLUGpendingfrom, ANDMODPLUGpendingto);
  }

  switch(when)
  {
    case com_peculiargames_andmodplug_PlayerThread_PATTERN_CHANGE_IMMEDIATE:
      ANDMODPLUGpatternfrom = ANDMODPLUGpendingfrom;
      ANDMODPLUGpatternto = ANDMODPLUGpendingto;
      break;
    case com_peculiargames_andmodplug_PlayerThread_PATTERN_CHANGE_AFTER_CURRENT:
      if (currmodFile != NULL)
        ANDMODPLUGpatternto = currmodFile->mSoundFile.GetCurrentPattern();
      break;
    case com_peculiargames_andmodplug_PlayerThread_PATTERN_CHANGE_AFTER_GROUP:
    default:
      break;
  }
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_SetCurrentPattern
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1SetCurrentPattern
  (JNIEnv *env, jobject obj, jint pattern)
{
  ANDMODPLUGnextpattern = pattern;
  ANDMODPLUGnextpatternmode = 1;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_SetNextPattern
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1SetNextPattern
  (JNIEnv *env, jobject obj, jint pattern)
{
  ANDMODPLUGnextpattern = pattern;
  ANDMODPLUGnextpatternmode = 0;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_LogOutput
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1LogOutput
  (JNIEnv *env, jclass cls, jboolean flag)
{
  if (flag == JNI_TRUE)
    ANDMODPLUGlogoutput = 1;
  else
    ANDMODPLUGlogoutput = 0;
}

/*
 * Class:     com_peculiargames_andmodplug_PlayerThread
 * Method:    ModPlug_SetLoopCount
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_peculiargames_andmodplug_PlayerThread_ModPlug_1SetLoopCount
  (JNIEnv *env, jclass cls, jint loopcount)
{
  ModPlug_Settings settings;

  ModPlug_GetSettings(&settings);
  settings.mLoopCount = loopcount;
  ModPlug_SetSettings(&settings);
}

// ************************************************************ 
// End of JNI stub code for libmodplug
// ************************************************************ 
