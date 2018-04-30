# Unobvious tests

if -1 < 2 > 1:
    # true
    pass

if -1 < 2 > 1 < 10 > -0 < 123 < 147 > 23 == 23 >= True == 1 <= 2 > False != 1 != 0:
    # true
    pass

if 0 < True > 0 and - 1 + (123 - 44) and False < 1 > 0 >= -1 <= True < (1 ** 123) + 322:
    # true
    pass
