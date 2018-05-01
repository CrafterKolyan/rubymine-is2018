# Float tests

if 0.0:
    # false
    pass

if -1.0:
    # true
    pass

if 1e-6:
    # true
    pass

if 1.0 + 0.5 < 1.6:
    # true
    pass

if 1.0 + 0.5 < -1.6:
    # false
    pass

if 0.0 == 0:
    # true
    pass

if 1.0 - 1.5 <= -0.4:
    # true
    pass

if 1.0 - 0.5 < 0.6:
    # true
    pass

if 1.0 - (0.5 + 0.2) - 0 > 0.6:
    # false
    pass