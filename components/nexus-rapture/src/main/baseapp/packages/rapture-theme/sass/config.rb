cur_dir = File.dirname(__FILE__)
output_style = :nested

# HACK: workaround to issue setting line_comment=false from require'd config inclusion
::Compass.configuration.line_comments = false