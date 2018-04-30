if not True:
    # false
    pass

if not False:
    # true
    pass

if True and True:
    # true
    pass

if True and False:
    # false
    pass

if False and True:
    # false
    pass

if False and False:
    # false
    pass

if True or True:
    # true
    pass

if True or False:
    # true
    pass

if False or True:
    # true
    pass

if False or False:
    # false
    pass

if 10 < 20 and 5 > 3:
    # true
    pass

if 10 < 20 or 5 <= 3:
    # true
    pass

if False or True and False:
    # false
    pass

if not((-1 < -2) or not(15 <= 13) and (1237512736 <= -12893618236)):
    # true
    pass

if not(False):
    # true
    pass

if (5 or 3) < 4:
    # false
    pass

if (0 and -3) > -2:
    # true
    pass

if 3 > 4 and 5 < 10:
    # false
    pass

if not(3 > 4) and 5 < 10:
    # true
    pass

if 0 == 0 and 0 != 1:
    # true
    pass