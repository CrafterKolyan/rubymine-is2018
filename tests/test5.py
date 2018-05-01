a = int(input())
b = int(input())

if a < 5 and a > -10:
    # undefined
    pass

if a < 5 and a > 12:
    # false
    pass

if a < 123 * 322 or a > 12 * (6 + 6) - 228:
    # true
    pass

if a < 5 or b > 10 or a > -5 or b < 12:
    # true
    pass

if a < 0 or a > 0:
    # undefined
    pass

if a < 0 and a > 0:
    # false
    pass

if a < -228 or a > 228:
    # undefined
    pass

if a < -228 and a > -228:
    # false
    pass