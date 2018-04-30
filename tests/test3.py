if 5 * 5 < 25:
    # false
    pass

if 5 * 5 < 13 + 13:
    # true
    pass

if -1 * -1 > -0 * (10 - 20):
    # true
    pass

if 10 - 20:
    # true
    pass

if (2 + 5) / 3 > 2:
    # true
    pass

if (1 + 2) * 3 == 9:
    # true
    pass

if (2 + 5) // 3 > 2:
    # false
    pass

if -7 % 3 == 2:
    # true
    pass

if 7 % 3 == 1:
    # true
    pass

if (True ^ 0) == 1:
    # true
    pass

if (-1 ^ True) == -2:
    # true
    pass

if ~True == -2:
    # true
    pass

if ~False == -1:
    # true
    pass

if ~13847628374638628973456893465 == -13847628374638628973456893466:
    # true
    pass

if (((1 << 2) | (-312893612846 >> 15)) ^ 12897 + (128 - 912397891724987128487) ** 2) % 1000000007 == 341987264:
    # true
    pass

if not(1 + 2 > 2 * 2) or 5 > 2 * 5:
    # true
    pass

if 1 << 0 == 1 and 1 >> 0 == 1 and -1 >> 32 == -1:
    # true
    pass