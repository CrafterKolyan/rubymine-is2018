# Unobvious tests

if +5 == 5:
    # true
    pass

if -1 < 2 > 1:
    # true
    pass

if 1 > 2 < 3:
    # false
    pass

if (4 < 5 > 3 and 5) == 5 and (4 > 5 < 3 and 5) == False:
    # true
    pass

a = int(input())
if 4 > 5 < a and a * a >= 0:
    # false
    pass

if -a == 0:
    # undefined
    pass

if a == 0 and a == 1 or a == 0:
    # undefined
    pass

if -1 < 2 > 1 < 10 > -0 < 123 < 147 > 23 == 23 >= True == 1 <= 2 > False != 1 != 0:
    # true
    pass

if 0 < True > 0 and - 1 + (123 - 44) and False < 1 > 0 >= -1 <= True < (1 ** 123) + 322:
    # true
    pass
elif -1 < 2 > 1:
    # true
    pass

if 5 % 3 == 2:
    # true
    pass
if -5 % 3 == 1:
    # true
    pass
if 5 % -3 == -1:
    # true
    pass
if -5 % -3 == -2:
    # true
    pass

if 5 % 3 == -1:
    # false
    pass
if -5 % 3 == -2:
    # false
    pass
if 5 % -3 == 1:
    # false
    pass
if -5 % -3 == 2:
    # false
    pass

if 1 // 0 == 15 or -10 % (5 - 10 // 2) + 15 == 17:
    # undefined behaviour. Here should be no RUNTIME
    # Two warnings: 1. Division by 0, 2. Taking modulo by 0
    pass

if (3 or -10 % 0) < 5:
    # true. Here should be no RUNTIME
    # Warning: Taking modulo by 0
    pass

if 1 >> -1 == 2:
    # undefined behaviour. Here should be no RUNTIME
    # Warning: Shifting by negative number (-1)
    pass

if 1 << (17 - 20) == 0:
    # undefined behaviour. Here should be no RUNTIME
    # Warning: Shifting by negative number (-3)
    pass

if 0 ** 0 == 1:
    # true. No undefined behaviour
    pass

if 0 ** 0 == 0:
    # false. No undefined behaviour
    pass

if (0 ** 1) == 0:
    # true
    pass

if 0 ** (25 // 6 - 5) == 1:
    # undefined behaviour. Here should be no RUNTIME
    # Warning: 0 cannot be raised to a negative power (-1)
    pass