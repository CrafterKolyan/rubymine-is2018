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

if 0.0 == 0 and 0 == -0.0:
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

if 7.5 // 1.8 == 4.0:
    # true
    pass

if -7.5 // 1.8 == -5.0:
    # true
    pass

if 7.5 // -1.8 == -5.0:
    # true
    pass

if -7.5 // -1.8 == 4.0:
    # true
    pass

if 7.5 % 1.8 < 0.3 + 1e-6 and 7.5 % 1.8 > 0.3 - 1e-6:
    # true
    pass

if -7.5 % 1.8 < 1.5 + 1e-6 and -7.5 % 1.8 > 1.5 - 1e-6:
    # true
    pass

if 7.5 % -1.8 < -1.5 + 1e-6 and 7.5 % -1.8 > -1.5 - 1e-6:
    # true
    pass

if -7.5 % -1.8 < -0.3 + 1e-6 and -7.5 % -1.8 > -0.3 - 1e-6:
    # true
    pass

if 1 % (1 / 3) < 1e-6 and 1 % (1 / 3) > -1e-6:
    # true
    pass

if 7.5 % 1.8 < -0.3 + 1e-6 and 7.5 % 1.8 > -0.3 - 1e-6:
    # false
    pass

if 1 + 18 % 2.3 < 0:
    # false
    pass

if 10 & 12.3:
    # undefined. no RUNTIME here
    pass

if 13.3 ^ 12.3:
    # undefined. no RUNTIME here
    pass

if 10 | 12.3:
    # undefined. no RUNTIME here
    pass

if 78462 <= 2.5 ** 12.3 <= 78463:
    # true
    # Won't work. BigDecimal can be powered only to Integer power.
    # Need extra libraries to support BigDecimal ** BigDecimal
    pass

if -1e81 < -4.5 ** 123 < -1e80:
    # true
    pass

if -2 - 1e-6 < -5.0 // 4.0 < -2 + 1e-6:
    # true
    pass

if -2 - 1e-6 < 5.0 // -4.0 < -2 + 1e-6:
    # true
    pass

if 1 - 1e-6 < 5.0 // 4.0 < 1 + 1e-6:
    # true
    pass

if 1 - 1e-6 < -5.0 // -4.0 < 1 + 1e-6:
    # true
    pass
