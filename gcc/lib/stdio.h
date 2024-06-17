/*
 * Copyright (c) 2018, Marcelo Samsoniuk
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdarg.h>
#include "io.h"
#include "uart.h"

#ifndef __STDIO__
#define __STDIO__

#define EOF   -1
#define NUL   0
#define NULL  (void *)0

// high-level functions use the getchar/putchar

char *gets(char *p, int s)
{
    char *ret = p;
    int c;

    while (--s)
    {
        c = getchar();

        if (c == '\n' || c == '\r')
            break;
        putchar(c);
        if (c == '\b') // backspace!
        {
            if (p != ret)
            {
                *--p = 0;
                s++;
            }
        }
        else
            *p++ = c;
    }
    putchar('\n');
    *p = 0;

    return p == ret ? NULL : ret;
}

void putstr(char *p)
{
    if (p)
        while (*p)
            putchar(*p++);
    else
        putstr("(NULL)");
}

int puts(char *p)
{
    putstr(p);
    return putchar('\n');
}

void putdx(unsigned i, int mode) // mode1 = dec, mode0 = hex
{
    char *hex = "0123456789abcdef";

    int dbd[] = {1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10, 1, 0};
    int dbx[] = {16777216, 65536, 256, 1, 0};

    int *db = mode ? dbd : dbx;

    if (mode && i < 0)
    {
        putchar('-');
        i = -1;
    }

    int j, k, l;

    for (j = 0, k = 24; (l = db[j]); j++, k -= 8)
    {
        if (l == 1 || i >= l)
        {
            if (mode)
            {
                putchar(hex[(i / l) % 10]);
            }
            else
            {
                putchar(hex[(i >> (k + 4)) & 15]);
                putchar(hex[(i >> k) & 15]);
            }
        }
    }
}

void putx(unsigned i)
{
    putdx(i, 0);
}

void putd(int i)
{
    putdx(i, 1);
}

int printf(char *fmt, ...)
{
    va_list ap;

    for (va_start(ap, fmt); *fmt; fmt++)
    {
        if (*fmt == '%')
        {
            fmt++;
            if (*fmt == 's')
                putstr(va_arg(ap, char *));
            else if (*fmt == 'x')
                putx(va_arg(ap, int));
            else if (*fmt == 'd')
                putd(va_arg(ap, int));
            else
                putchar(*fmt);
        }
        else
            putchar(*fmt);
    }

    va_end(ap);

    return 0;
}

int strncmp(char *s1, char *s2, int len)
{
    while (--len && *s1 && *s2 && (*s1 == *s2))
        s1++, s2++;

    return (*s1 - *s2);
}

int strcmp(char *s1, char *s2)
{
    return strncmp(s1, s2, -1);
}

int strlen(char *s1)
{
    int len;

    for (len = 0; s1 && *s1++; len++)
        ;

    return len;
}

char *memcpy(char *dptr, char *sptr, int len)
{
    char *ret = dptr;

    while (len--)
        *dptr++ = *sptr++;

    return ret;
}

char *memset(char *dptr, int c, int len)
{
    char *ret = dptr;

    while (len--)
        *dptr++ = c;

    return ret;
}

char *strtok(char *str, char *dptr)
{
    static char *nxt = NULL;

    int dlen = strlen(dptr);
    char *tmp;

    if (str)
        tmp = str;
    else if (nxt)
        tmp = nxt;
    else
        return NULL;

    char *ret = tmp;

    while (*tmp)
    {
        if (strncmp(tmp, dptr, dlen) == 0)
        {
            *tmp = NUL;
            nxt = tmp + 1;
            return ret;
        }
        tmp++;
    }
    nxt = NULL;
    return ret;
}

int atoi(char *s1)
{
    int ret, sig;

    for (sig = ret = 0; s1 && *s1; s1++)
    {
        if (*s1 == '-')
            sig = 1;
        else
            ret = *s1 - '0' + (ret << 3) + (ret << 1); // val = val*10+int(*s1)
    }

    return sig ? -ret : ret;
}

int xtoi(char *s1)
{
    int ret;

    for (ret = 0; s1 && *s1; s1++)
    {
        if (*s1 <= '9')
            ret = *s1 - '0' + (ret << 4); // val = val*16+int(*s1)
        else
            ret = 10 + (*s1 & 0x5f) - 'A' + (ret << 4); // val = val*16+int(toupper(*s1))
    }

    return ret;
}

// int mac(int acc,short x,short y)
// {
// #ifdef __RISCV__
//     __asm__(".word 0x00c5857F"); // mac a0,a1,a2
//     // "template"
//     //acc += (x^y);
// #else
//     acc+=x*y;
// #endif
//     return acc;
// }

unsigned __umulsi3(unsigned x, unsigned y)
{
    unsigned acc;

    if (x < y)
    {
        unsigned z = x;
        x = y;
        y = z;
    }

    for (acc = 0; y; x <<= 1, y >>= 1)
        if (y & 1)
            acc += x;

    return acc;
}

int __mulsi3(int x, int y)
{
    unsigned acc, xs, ys;

    if (x < 0)
    {
        xs = 1;
        x = -x;
    }
    else
        xs = 0;
    if (y < 0)
    {
        ys = 1;
        y = -y;
    }
    else
        ys = 0;

    acc = __umulsi3(x, y);

    return xs ^ ys ? -acc : acc;
}

unsigned __udiv_umod_si3(unsigned x, unsigned y, int opt)
{
    unsigned acc, aux;

    if (!y)
        return 0;

    for (aux = 1; y < x && !(y & (1 << 31)); aux <<= 1, y <<= 1)
        ;
    for (acc = 0; x && aux; aux >>= 1, y >>= 1)
        if (y <= x)
            x -= y, acc += aux;

    return opt ? acc : x;
}

int __udivsi3(int x, int y)
{
    return __udiv_umod_si3(x, y, 1);
}

int __umodsi3(int x, int y)
{
    return __udiv_umod_si3(x, y, 0);
}

int __div_mod_si3(int x, int y, int opt)
{
    unsigned acc, xs, ys;

    if (!y)
        return 0;

    if (x < 0)
    {
        xs = 1;
        x = -x;
    }
    else
        xs = 0;
    if (y < 0)
    {
        ys = 1;
        y = -y;
    }
    else
        ys = 0;

    acc = __udiv_umod_si3(x, y, opt);

    if (opt)
        return xs ^ ys ? -acc : acc;
    else
        return xs ? -acc : acc;
}

int __divsi3(int x, int y)
{
    return __div_mod_si3(x, y, 1);
}

int __modsi3(int x, int y)
{
    return __div_mod_si3(x, y, 0);
}


#endif
