#pragma once

#include <cstdint>
#include <cstring>

#ifndef _WINDOWS_
#define _WINDOWS_

typedef int BOOL;
typedef unsigned int DWORD;
typedef unsigned int ULONG;
typedef unsigned int UINT;
typedef int LONG;
typedef unsigned long long ULONGLONG;
typedef void* LPVOID;
typedef void* PVOID;
typedef unsigned char BYTE;
typedef const char* LPCSTR;
typedef wchar_t WCHAR;
typedef const WCHAR* LPWSTR;
typedef const WCHAR* LPCWSTR;
typedef void* HANDLE;
typedef void* HMODULE;
typedef uintptr_t UINT_PTR;
typedef uint32_t ULONG32;

#define STDMETHODCALLTYPE
#define __stdcall

struct GUID {
    uint32_t       Data1;
    uint16_t       Data2;
    uint16_t       Data3;
    unsigned char  Data4[8];

    bool operator==(const GUID& other) const {
        return Data1 == other.Data1 && Data2 == other.Data2 &&
               Data3 == other.Data3 && memcmp(Data4, other.Data4, 8) == 0;
    }
    bool operator!=(const GUID& other) const {
        return !(*this == other);
    }
};
typedef GUID IID;
typedef GUID CLSID;
#define REFGUID const GUID &
#define REFIID const IID &
#define REFCLSID const CLSID &

static const IID IID_IUnknown =
{ 0x00000000, 0x0000, 0x0000, { 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46 } };

static const IID IID_IClassFactory =
{ 0x00000001, 0x0000, 0x0000, { 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46 } };

typedef long HRESULT;
#define S_OK 0L
#define S_FALSE 1L
#define E_FAIL 0x80004005L
#define E_NOINTERFACE 0x80004002L
#define E_POINTER 0x80004003L
#define CLASS_E_NOAGGREGATION 0x80040110L
#define CLASS_E_CLASSNOTAVAILABLE 0x80040111L
#define META_E_BAD_SIGNATURE 0x80131192L

#define SUCCEEDED(hr) (((HRESULT)(hr)) >= 0)
#define FAILED(hr) (((HRESULT)(hr)) < 0)

#define EXTERN_GUID(itf,l1,s1,s2,c1,c2,c3,c4,c5,c6,c7,c8)  \
  static const IID itf = {l1,s1,s2,{c1,c2,c3,c4,c5,c6,c7,c8}}

struct IUnknown {
    virtual HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void **ppvObject) = 0;
    virtual ULONG STDMETHODCALLTYPE AddRef() = 0;
    virtual ULONG STDMETHODCALLTYPE Release() = 0;
};

struct IClassFactory : public IUnknown {
    virtual HRESULT STDMETHODCALLTYPE CreateInstance(IUnknown *pUnkOuter, REFIID riid, void **ppvObject) = 0;
    virtual HRESULT STDMETHODCALLTYPE LockServer(BOOL fLock) = 0;
};

// Mock specstrings
#define _Out_writes_to_opt_(a,b)
#define _Out_writes_to_(a,b)

#define MAX_PATH 260
#define UNALIGNED

#endif
